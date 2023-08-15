@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "ConvertTryFinallyToUseCall", "SqlDialectInspection")

package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookState.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.jetbrains.concurrency.runAsync
import java.io.RandomAccessFile
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/**
 * Word book service.
 */
@Service
class WordBookService : Disposable {

    private lateinit var queryRunner: QueryRunner

    @Volatile
    private var classLoader: ClassLoader? = null
        set(value) {
            field = requireNotNull(value) { "Cannot set a null value" }
        }

    private val isSubscribed = AtomicBoolean(false)

    private val wordBookPublisher: WordBookListener = Application.messageBus.syncPublisher(WordBookListener.TOPIC)

    private val observableState: ObservableValue<WordBookState> =
        object : ObservableValue<WordBookState>(UNINITIALIZED) {
            override fun notifyChanged(oldValue: WordBookState, newValue: WordBookState) {
                LOGGER.i("Wordbook service state changed: $oldValue -> $newValue")
                if (isStableState(newValue)) {
                    subscribeStoragePathChanges()
                }
                invokeLaterIfNeeded(ModalityState.any()) { super.notifyChanged(oldValue, newValue) }
            }
        }

    /**
     * The observable state binding of service, state changes will be notified in the EDT.
     */
    val stateBinding: Observable<WordBookState> =
        object : ObservableValue.ReadOnlyWrapper<WordBookState>(observableState) {
            override val value: WordBookState
                get() = synchronized(this@WordBookService) { super.value }
        }

    var state: WordBookState by observableState
        @Synchronized get
        private set

    val isInitialized: Boolean
        @Synchronized get() = state == RUNNING


    init {
        asyncInitialize()
    }

    private fun subscribeStoragePathChanges() {
        if (!isSubscribed.compareAndSet(false, true)) {
            return
        }

        Application.messageBus
            .connect(this)
            .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                override fun onWordbookStoragePathChanged(settings: Settings) {
                    onStoragePathChanged(settings.wordbookStoragePath)
                }
            })
    }

    @Synchronized
    private fun nextState(nextState: WordBookState, vararg preState: WordBookState): Boolean {
        return if (preState.isEmpty() || state in preState) {
            state = nextState
            true
        } else false
    }

    private fun onStoragePathChanged(newPath: String?) {
        synchronized(this@WordBookService) {
            check(isStableState(state))
            if (state != RUNNING) {
                asyncInitialize()
                return
            }
        }

        runAsync {
            val newDbFile = newPath?.takeIf { it.isNotBlank() }
                ?.let { getStorageFile(Paths.get(it)) }
                ?: getStorageFile(TranslationStorages.DATA_DIRECTORY)
            Files.createDirectories(newDbFile.parent)

            val runner = createRunner(newDbFile)
            synchronized(this@WordBookService) {
                queryRunner = runner
            }

            invokeLater(ModalityState.any()) { wordBookPublisher.onStoragePathChanged(this@WordBookService) }
        }.onError { error ->
            nextState(INITIALIZATION_ERROR)
            val errorMsg = (error as? SQLException)
                ?.let { WordBookErrorCode[it.errorCode].reason }
                ?: error.message ?: ""
            val title = message("wordbook.service.notification.title")
            val message =
                message("wordbook.service.notification.message.failed.to.switch.storage.path", errorMsg)
            invokeLater(ModalityState.NON_MODAL) {
                LOGGER.w("Failed to switch storage path", error)
                Notifications.showErrorNotification(title, message)
            }
        }
    }

    fun asyncInitialize() {
        val latch = CountDownLatch(1)
        try {
            runAsync {
                // 确保外部各种回调都准备好了再执行后台任务
                latch.await()
                if (!nextState(INITIALIZING, UNINITIALIZED, INITIALIZATION_ERROR)) {
                    return@runAsync
                }
                if (classLoader == null) {
                    findDriverClassLoader()?.let { classLoader = it }
                }
                if (classLoader != null) {
                    initService()
                } else {
                    nextState(NO_DRIVER)
                }
            }.onError {
                LOGGER.w("Wordbook initialization failed", it)
                nextState(INITIALIZATION_ERROR)
            }
        } finally {
            latch.countDown()
        }
    }

    private fun initService() {
        val dbFile = Settings.instance.wordbookStoragePath
            ?.takeIf { it.isNotBlank() }
            ?.let { getStorageFile(Paths.get(it)) }
            ?: getStorageFile(TranslationStorages.DATA_DIRECTORY).also { dbFile ->
                TranslationStorages.createDataDirectoriesIfNotExists()
                lock { migrateDatabaseIfNeed(dbFile) }
            }

        val runner = createRunner(dbFile)
        synchronized(this) {
            queryRunner = runner
            nextState(RUNNING)
        }
    }

    private fun createRunner(dbFile: Path): QueryRunner {
        val clazz = Class.forName(SQLITE_DATA_SOURCE, true, classLoader)
        val dataSource: DataSource = clazz.getConstructor().newInstance() as DataSource
        val setUrlMethod = clazz.getMethod("setUrl", String::class.java)
        setUrlMethod.invoke(dataSource, DATABASE_URL_PREFIX + dbFile)

        return QueryRunner(dataSource).apply { initTable() }
    }

    private fun findDriverClassLoader(): ClassLoader? {
        val defaultClassLoader: ClassLoader? = javaClass.classLoader
        if (defaultClassLoader?.canDriveService() == true) {
            return defaultClassLoader
        }

        TranslationStorages.createDataDirectoriesIfNotExists()
        return lock {
            if (!checkDriverFile()) {
                return@lock null
            }

            val urlClassLoader = URLClassLoader(arrayOf(DRIVER_JAR.toUri().toURL()), defaultClassLoader)
            return@lock if (urlClassLoader.canDriveService(false)) {
                urlClassLoader
            } else {
                Files.delete(DRIVER_JAR)
                null
            }
        }
    }

    fun downloadDriverAndInitService(): Boolean {
        if (!nextState(DOWNLOADING_DRIVER, NO_DRIVER)) {
            return false
        }

        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(null, message("word.book.progress.downloading.driver"), true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    downloadDriverFile(indicator)

                    nextState(INITIALIZING)
                    indicator.isIndeterminate = true
                    indicator.text = message("word.book.progress.initializing.service")

                    findDriverClassLoader()?.let { classLoader = it }
                    if (classLoader != null) {
                        initService()
                    } else {
                        nextState(NO_DRIVER)
                    }
                }

                override fun onCancel() {
                    nextState(NO_DRIVER)
                }

                override fun onThrowable(error: Throwable) {
                    val state = synchronized(this@WordBookService) {
                        val nextState = if (classLoader != null) INITIALIZATION_ERROR else NO_DRIVER
                        nextState(nextState)
                        nextState
                    }

                    if (state == NO_DRIVER) {
                        LOGGER.w("Failed to download the driver file", error)
                        Notifications.showErrorNotification(
                            message("wordbook.notification.title"),
                            message("wordbook.window.message.driver.download.failed", error.message.toString())
                        )
                    } else {
                        LOGGER.w("Wordbook initialization failed", error)
                    }
                }
            })

        return true
    }

    private fun downloadDriverFile(indicator: ProgressIndicator) {
        indicator.text = message("word.book.progress.downloading")
        indicator.checkCanceled()

        TranslationStorages.createDataDirectoriesIfNotExists()
        lock {
            if (checkDriverFile()) {
                return
            }
            Files.deleteIfExists(DRIVER_JAR)
        }

        var downloadedFile: Path? = null
        try {
            val tempFile = Files.createTempFile(TranslationStorages.DATA_DIRECTORY, "download.", ".tmp")
            downloadedFile = tempFile

            HttpRequests.request(DRIVER_FILE_URL).saveToFile(tempFile.toFile(), indicator)
            indicator.checkCanceled()
            indicator.fraction = 1.0
            indicator.isIndeterminate = true

            lock {
                if (!checkDriverFile()) {
                    Files.move(tempFile, DRIVER_JAR, StandardCopyOption.REPLACE_EXISTING)

                    if (!checkDriverFile()) {
                        Files.deleteIfExists(DRIVER_JAR)
                    }
                }
            }
        } finally {
            try {
                downloadedFile?.let { Files.deleteIfExists(it) }
            } catch (e: Exception) {
                LOGGER.w("Failed to delete temporary file", e)
            }
        }
    }

    /**
     * Test if the specified [word] can be added to the wordbook
     */
    fun canAddToWordbook(word: String?): Boolean {
        return !word.isNullOrBlank() && word.length <= 60 && '\n' !in word
    }

    private fun checkIsInitialized() = check(isInitialized) { "Word book not initialized" }

    private fun QueryRunner.initTable() {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS wordbook (
                "$COLUMN_ID"            INTEGER  PRIMARY KEY,
                $COLUMN_WORD            TEXT     COLLATE NOCASE NOT NULL,
                $COLUMN_SOURCE_LANGUAGE TEXT                    NOT NULL,
                $COLUMN_TARGET_LANGUAGE TEXT                    NOT NULL,
                $COLUMN_PHONETIC        TEXT,
                $COLUMN_EXPLANATION     TEXT,
                $COLUMN_TAGS            TEXT,
                $COLUMN_CREATED_AT      DATETIME                NOT NULL
            )
        """.trimIndent()
        update(createTableSQL)

        val createIndexSQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS wordbook_unique_index
                ON wordbook ($COLUMN_WORD, $COLUMN_SOURCE_LANGUAGE, $COLUMN_TARGET_LANGUAGE)
        """.trimIndent()
        update(createIndexSQL)
    }

    /**
     * Adds the specified [word] to the word book and returns the id if the [word] is inserted.
     *
     * @see WordBookListener.onWordsAdded
     * @throws WordBookException if a wordbook access error occurs
     */
    fun addWord(word: WordBookItem): Long? {
        checkIsInitialized()

        return try {
            insertWord(word)
        } catch (e: SQLException) {
            val id = if (e.errorCode == SQLITE_CONSTRAINT) {
                findWordId(word.word, word.sourceLanguage, word.targetLanguage)
            } else null
            id ?: e.rethrow("Unable to add word: ${word.word}")
        }?.also {
            word.id = it
            invokeAndWait(ModalityState.any()) {
                wordBookPublisher.onWordsAdded(this@WordBookService, listOf(word))
            }
        }
    }

    /**
     * Inserts the specified [word] to the word book and returns the id if the [word] is inserted.
     */
    internal fun insertWord(word: WordBookItem): Long? {
        checkIsInitialized()

        val sql = """
            INSERT INTO wordbook (
                $COLUMN_WORD,
                $COLUMN_SOURCE_LANGUAGE,
                $COLUMN_TARGET_LANGUAGE,
                $COLUMN_PHONETIC,
                $COLUMN_EXPLANATION,
                $COLUMN_TAGS,
                $COLUMN_CREATED_AT
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return queryRunner.insert(
            sql,
            WordIdHandler,
            word.word,
            word.sourceLanguage.code,
            word.targetLanguage.code,
            word.phonetic,
            word.explanation,
            word.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
            word.createdAt
        )?.also { word.id = it }
    }

    /**
     * Update the specified word to the word book.
     *
     * @see WordBookListener.onWordsUpdated
     * @throws WordBookException if a wordbook access error occurs
     */
    fun updateWord(word: WordBookItem): Boolean {
        checkIsInitialized()

        val id = requireNotNull(word.id) { "Required word id was null." }
        val sql = """
            UPDATE wordbook
            SET
                $COLUMN_PHONETIC = ?,
                $COLUMN_EXPLANATION = ?,
                $COLUMN_TAGS = ?
            WHERE $COLUMN_ID = ?
        """.trimIndent()
        val updated = try {
            queryRunner.update(
                sql,
                word.phonetic,
                word.explanation,
                word.tags.joinToString(","),
                id
            ) > 0
        } catch (e: SQLException) {
            e.rethrow("Unable to update word: ${word.word}")
        }

        if (updated) {
            invokeAndWait(ModalityState.any()) {
                wordBookPublisher.onWordsUpdated(this@WordBookService, listOf(word))
            }
        }

        return updated
    }

    /**
     * Removes the word by the specified [id].
     *
     * @see WordBookListener.onWordsRemoved
     * @throws WordBookException if a wordbook access error occurs
     */
    fun removeWord(id: Long) {
        checkIsInitialized()

        val sql = """
            DELETE FROM wordbook
            WHERE $COLUMN_ID = $id
        """.trimIndent()
        try {
            queryRunner.update(sql)
        } catch (e: SQLException) {
            e.rethrow("Unable to remove word: $id")
        }

        invokeAndWait(ModalityState.any()) {
            wordBookPublisher.onWordsRemoved(this@WordBookService, listOf(id))
        }
    }

    /**
     * Removes words by the specified [ids].
     *
     * @see WordBookListener.onWordsRemoved
     * @throws WordBookException if a wordbook access error occurs
     */
    fun removeWords(ids: List<Long>) {
        checkIsInitialized()

        val sql = """
            DELETE FROM wordbook
            WHERE $COLUMN_ID IN (${ids.joinToString()})
        """.trimIndent()
        try {
            queryRunner.update(sql)
        } catch (e: SQLException) {
            e.rethrow("Unable to remove words: $ids")
        }

        invokeAndWait(ModalityState.any()) {
            wordBookPublisher.onWordsRemoved(this@WordBookService, ids)
        }
    }

    /**
     * Returns the word ID by the specified [word], [source language][sourceLanguage] and [target language][targetLanguage].
     */
    fun getWordId(word: String, sourceLanguage: Lang, targetLanguage: Lang): Long? {
        checkIsInitialized()

        val wordToQuery = word.trim()
        if (wordToQuery.isEmpty()) {
            return null
        }

        return findWordId(wordToQuery, sourceLanguage, targetLanguage)
    }

    private fun findWordId(word: String, sourceLanguage: Lang, targetLanguage: Lang): Long? {
        val sql = """
            SELECT $COLUMN_ID
            FROM wordbook 
            WHERE $COLUMN_WORD = ?
                AND $COLUMN_SOURCE_LANGUAGE = ?
                AND $COLUMN_TARGET_LANGUAGE = ?
        """.trimIndent()
        return try {
            queryRunner.query(sql, WordIdHandler, word, sourceLanguage.code, targetLanguage.code)
        } catch (e: SQLException) {
            LOGGER.w("Failed to find word id", e)
            null
        }
    }

    /**
     * Returns all words.
     */
    fun getWords(): List<WordBookItem> {
        checkIsInitialized()

        val sql = """
            SELECT *
            FROM wordbook
            ORDER BY $COLUMN_CREATED_AT DESC
        """.trimIndent()
        return try {
            queryRunner.query(sql, WordListHandler)
        } catch (e: SQLException) {
            LOGGER.w("Failed to get words", e)
            emptyList()
        }
    }

    fun hasWords(): Boolean {
        checkIsInitialized()

        val sql = "SELECT COUNT(*) FROM wordbook"
        return try {
            queryRunner.query(sql, BooleanHandler)
        } catch (e: SQLException) {
            LOGGER.w(e.message ?: "", e)
            false
        }
    }

    override fun dispose() {
    }


    private object BooleanHandler : ResultSetHandler<Boolean> {
        override fun handle(rs: ResultSet): Boolean = rs.takeIf { it.next() }?.getBoolean(1) ?: false
    }

    private object WordIdHandler : ResultSetHandler<Long?> {
        override fun handle(rs: ResultSet): Long? = rs.takeIf { it.next() }?.getLong(1)
    }

    private object WordListHandler : ResultSetHandler<List<WordBookItem>> {
        override fun handle(rs: ResultSet): List<WordBookItem> {
            return generateSequence { rs.takeIf { it.next() }?.toWordBookItem() }.toList()
        }
    }


    companion object {
        private const val DRIVER_VERSION = "3.39.3.0"
        private const val STORAGE_FILE_NAME = "wordbook.sqlite"

        private val LOCK_FILE = TranslationStorages.DATA_DIRECTORY.resolve(".lock")
        private val DRIVER_JAR = TranslationStorages.DATA_DIRECTORY.resolve("driver-v$DRIVER_VERSION.jar")

        // https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/
        private const val DRIVER_FILE_URL =
            "https://maven.aliyun.com/repository/public/org/xerial/sqlite-jdbc/$DRIVER_VERSION/sqlite-jdbc-$DRIVER_VERSION.jar"

        // https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.39.3.0/sqlite-jdbc-3.39.3.0.jar.sha1
        private const val DRIVER_FILE_SHA1 = "94166806682e738a5275bd09052fa34b1328eedf"

        private const val SQLITE_DATA_SOURCE = "org.sqlite.SQLiteDataSource"
        private const val DATABASE_URL_PREFIX = "jdbc:sqlite:"

        // org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT.code = 19
        private const val SQLITE_CONSTRAINT = 19

        private const val COLUMN_ID = "_id"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_SOURCE_LANGUAGE = "source_language"
        private const val COLUMN_TARGET_LANGUAGE = "target_language"
        private const val COLUMN_PHONETIC = "phonetic"
        private const val COLUMN_EXPLANATION = "explanation"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_CREATED_AT = "created_at"

        private val LOGGER = Logger.getInstance(WordBookService::class.java)

        val instance: WordBookService
            get() = ApplicationManager.getApplication().getService(WordBookService::class.java)

        fun isStableState(state: WordBookState): Boolean {
            return state == NO_DRIVER || state == INITIALIZATION_ERROR || state == RUNNING
        }

        fun getStorageFile(dir: Path = TranslationStorages.DATA_DIRECTORY): Path = dir.resolve(STORAGE_FILE_NAME)

        private inline fun <T> lock(block: () -> T): T {
            return RandomAccessFile(LOCK_FILE.toFile(), "rw").use { lockFile ->
                val lock = lockFile.channel.lock()
                try {
                    block()
                } finally {
                    lock.release()
                }
            }
        }

        private fun checkDriverFile(): Boolean {
            return try {
                Files.exists(DRIVER_JAR) && DRIVER_FILE_SHA1.equals(DRIVER_JAR.sha1(), true)
            } catch (e: Throwable) {
                LOGGER.w("Failed to check driver file.", e)
                false
            }
        }

        private fun ClassLoader.canDriveService(default: Boolean = true): Boolean {
            // 内置的SQLite驱动不支持Mac M1
            if (default && SystemInfo.isMac && /* Mac M1 */ SystemInfo.OS_ARCH.equals("aarch64", ignoreCase = true)) {
                return false
            }

            return try {
                Class.forName(SQLITE_DATA_SOURCE, false, this)
                true
            } catch (e: Throwable) {
                false
            }
        }

        private fun ResultSet.toWordBookItem(): WordBookItem {
            return WordBookItem(
                getLong(COLUMN_ID),
                getString(COLUMN_WORD),
                Lang[getString(COLUMN_SOURCE_LANGUAGE)],
                Lang[getString(COLUMN_TARGET_LANGUAGE)],
                getString(COLUMN_PHONETIC),
                getString(COLUMN_EXPLANATION),
                getString(COLUMN_TAGS),
                getDate(COLUMN_CREATED_AT)
            )
        }

        private fun SQLException.rethrow(message: String): Nothing {
            LOGGER.w(message, this)
            throw WordBookException(WordBookErrorCode[errorCode], message, this)
        }

        // Will be removed on v4.0
        private fun migrateDatabaseIfNeed(dbFile: Path) {
            LOGGER.i("Start migrating the wordbook database file.")
            try {
                if (Files.exists(dbFile)) {
                    LOGGER.i("The wordbook database file has been migrated.")
                    return
                }

                val userHomePath = System.getProperty("user.home")
                val defaultDir = Paths.get(userHomePath, ".translation")
                val oldDir = if (SystemInfo.isLinux && !Files.exists(defaultDir)) {
                    System.getenv("XDG_DATA_HOME")
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { Paths.get(it, ".translation") }
                        ?: defaultDir
                } else defaultDir

                val oldDbFile = oldDir.resolve("wordbook.db")
                if (!Files.exists(oldDbFile)) {
                    LOGGER.i("No wordbook database file to migrate.")
                    return
                }

                try {
                    RandomAccessFile(oldDir.resolve(".lock").toFile(), "rw").use { lockFile ->
                        val lock = lockFile.channel.lock()
                        try {
                            Files.copy(oldDbFile, dbFile)
                        } finally {
                            lock.release()
                        }
                    }
                } catch (e: Throwable) {
                    LOGGER.w(
                        "Cannot migrate the wordbook database file securely, directly through non-secure migration.",
                        e
                    )
                    Files.copy(oldDbFile, dbFile)
                }

                LOGGER.i("The wordbook database file has been successfully migrated.")
            } catch (e: Throwable) {
                LOGGER.w("Failed to migrate the wordbook database.", e)
            }
        }
    }
}