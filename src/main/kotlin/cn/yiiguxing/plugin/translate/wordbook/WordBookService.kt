@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "ConvertTryFinallyToUseCall", "SqlDialectInspection")

package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.TRANSLATION_DIRECTORY
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.wordbook.WordBookState.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.jetbrains.concurrency.runAsync
import java.io.RandomAccessFile
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Word book service.
 */
class WordBookService {

    private lateinit var queryRunner: QueryRunner

    private val wordBookPublisher: WordBookListener = Application.messageBus.syncPublisher(WordBookListener.TOPIC)

    private val observableState: ObservableValue<WordBookState> =
        object : ObservableValue<WordBookState>(UNINITIALIZED) {
            override fun notifyChanged(oldValue: WordBookState, newValue: WordBookState) {
                LOGGER.i("Wordbook service state changed: $oldValue -> $newValue")
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

    @Synchronized
    private fun nextState(nextState: WordBookState, vararg preState: WordBookState): Boolean {
        return if (preState.isEmpty() || state in preState) {
            state = nextState
            true
        } else false
    }

    fun asyncInitialize() {
        runAsync {
            if (!nextState(INITIALIZING, UNINITIALIZED, INITIALIZATION_ERROR)) {
                return@runAsync
            }

            if (!Files.exists(TRANSLATION_DIRECTORY)) {
                Files.createDirectories(TRANSLATION_DIRECTORY)
            }

            initService()
        }.onError {
            LOGGER.w("Wordbook initialization failed", it)
            nextState(INITIALIZATION_ERROR)
        }
    }

    private fun initService() {
        if (initQueryRunner()) {
            initTable()
            nextState(RUNNING)
        } else {
            nextState(NO_DRIVER)
        }
    }

    @Synchronized
    private fun initQueryRunner(): Boolean {
        if (::queryRunner.isInitialized) {
            return true
        }

        val classLoader = findDriverClassLoader() ?: return false
        val dataSource = BasicDataSource().apply {
            url = DATABASE_URL
            driverClassLoader = classLoader
            driverClassName = DATABASE_DRIVER
            connectionFactoryClassName = CONNECTION_FACTORY_CLASS
            defaultQueryTimeout = QUERY_TIMEOUT
        }
        queryRunner = QueryRunner(dataSource)
        return true
    }

    private fun findDriverClassLoader(): ClassLoader? {
        val defaultClassLoader: ClassLoader? = javaClass.classLoader
        if (defaultClassLoader?.canDriveService() == true) {
            return defaultClassLoader
        }

        return driverLock {
            if (!Files.exists(DRIVER_JAR)) {
                return@driverLock null
            }

            val urlClassLoader = URLClassLoader(arrayOf(DRIVER_JAR.toUri().toURL()), defaultClassLoader)
            return@driverLock if (urlClassLoader.canDriveService(false)) {
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
                    initService()
                }

                override fun onCancel() {
                    nextState(NO_DRIVER)
                }

                override fun onThrowable(error: Throwable) {
                    val state = synchronized(this@WordBookService) {
                        val nextState = if (state == INITIALIZING) INITIALIZATION_ERROR else NO_DRIVER
                        nextState(nextState)
                        nextState
                    }

                    if (state == NO_DRIVER) {
                        Notifications.showErrorNotification(
                            message("wordbook.notification.title"),
                            message("wordbook.window.message.driver.download.failed", error.message.toString())
                        )
                    }
                }
            })

        return true
    }

    private fun downloadDriverFile(indicator: ProgressIndicator) {
        indicator.text = message("word.book.progress.downloading")
        indicator.checkCanceled()
        var downloadedFile: Path? = null
        try {
            val tempFile = Files.createTempFile(TRANSLATION_DIRECTORY, "download.", ".tmp")
            downloadedFile = tempFile

            HttpRequests.request(DRIVER_FILE_URL).saveToFile(tempFile.toFile(), indicator)
            indicator.checkCanceled()

            driverLock {
                if (!Files.exists(DRIVER_JAR) || Files.size(DRIVER_JAR) != Files.size(tempFile)) {
                    Files.move(tempFile, DRIVER_JAR, StandardCopyOption.REPLACE_EXISTING)
                }
            }

            indicator.fraction = 1.0
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

    private fun initTable() {
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
        queryRunner.update(createTableSQL)

        val createIndexSQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS wordbook_unique_index
                ON wordbook ($COLUMN_WORD, $COLUMN_SOURCE_LANGUAGE, $COLUMN_TARGET_LANGUAGE)
        """.trimIndent()
        queryRunner.update(createIndexSQL)
    }

    /**
     * Adds the specified word to the word book and returns id if word is inserted.
     */
    fun addWord(item: WordBookItem, notifyOnFailed: Boolean = true): Long? {
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
        return try {
            queryRunner.insert(
                sql,
                WordIdHandler,
                item.word,
                item.sourceLanguage.code,
                item.targetLanguage.code,
                item.phonetic,
                item.explanation,
                item.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                item.createdAt
            )
        } catch (e: SQLException) {
            if (e.errorCode == SQLITE_CONSTRAINT) {
                findWordId(item.word, item.sourceLanguage, item.targetLanguage)
            } else {
                LOGGER.w("Insert word", e)
                if (notifyOnFailed) {
                    Notifications.showErrorNotification(
                        message("wordbook.notification.title"),
                        message("wordbook.notification.content.addFailed")
                    )
                }
                null
            }
        }?.also {
            item.id = it
            invokeAndWait(ModalityState.any()) {
                wordBookPublisher.onWordAdded(this@WordBookService, item)
            }
        }
    }

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
        val updated = queryRunner.update(
            sql,
            word.phonetic,
            word.explanation,
            word.tags.joinToString(","),
            id
        ) > 0

        if (updated) {
            invokeAndWait(ModalityState.any()) {
                wordBookPublisher.onWordUpdated(this@WordBookService, word)
            }
        }

        return updated
    }

    /**
     * Updates the [tags] of the word by the specified [id].
     */
    @Suppress("unused")
    fun updateTags(id: Long, tags: List<String>): Boolean {
        checkIsInitialized()

        val sql = """
            UPDATE wordbook
            SET
                $COLUMN_TAGS = ?
            WHERE $COLUMN_ID = ?
        """.trimIndent()
        return queryRunner.update(sql, tags.joinToString(","), id) > 0
    }

    /**
     * Removes the word by the specified [id].
     */
    fun removeWord(id: Long): Boolean {
        checkIsInitialized()

        val sql = """
            DELETE FROM wordbook
            WHERE $COLUMN_ID = $id
        """.trimIndent()
        val removed = queryRunner.update(sql) > 0

        if (removed) {
            invokeAndWait(ModalityState.any()) {
                wordBookPublisher.onWordRemoved(this@WordBookService, id)
            }
        }

        return removed
    }

    /**
     * Removes words by the specified [ids].
     */
    fun removeWords(ids: List<Long>): Boolean {
        checkIsInitialized()

        val sql = """
            DELETE FROM wordbook
            WHERE $COLUMN_ID IN (${ids.joinToString()})
        """.trimIndent()
        val removed = queryRunner.update(sql) > 0

        if (removed) {
            invokeAndWait(ModalityState.any()) {
                for (id in ids) {
                    wordBookPublisher.onWordRemoved(this@WordBookService, id)
                }
            }
        }

        return removed
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
            LOGGER.w("Failed to get all words", e)
            emptyList()
        }
    }

    fun hasAnyWords(): Boolean {
        checkIsInitialized()

        val sql = "SELECT COUNT(*) FROM wordbook"
        return try {
            queryRunner.query(sql, BooleanHandler)
        } catch (e: SQLException) {
            LOGGER.w(e.message ?: "", e)
            false
        }
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
        private const val DRIVER_VERSION = "3.36.0.3"

        private const val DRIVER_FILE_URL =
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$DRIVER_VERSION/sqlite-jdbc-$DRIVER_VERSION.jar"
        private const val DRIVER_FILE_NAME = "driver-v$DRIVER_VERSION.jar"
        private const val DRIVER_LOCK_FILE = "driver-v$DRIVER_VERSION.jar.lock"
        private val DRIVER_JAR = TRANSLATION_DIRECTORY.resolve(DRIVER_FILE_NAME)

        private const val DATABASE_DRIVER = "org.sqlite.JDBC"
        private val DATABASE_URL = "jdbc:sqlite:${TRANSLATION_DIRECTORY.resolve("wordbook.db")}"

        private const val QUERY_TIMEOUT = 5 // timeout in seconds
        private const val CONNECTION_FACTORY_CLASS =
            "cn.yiiguxing.plugin.translate.wordbook.WordBookDriverConnectionFactory"

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

        private inline fun <T> driverLock(block: () -> T): T {
            return RandomAccessFile(TRANSLATION_DIRECTORY.resolve(DRIVER_LOCK_FILE).toFile(), "rw").use { lockFile ->
                val lock = lockFile.channel.lock()
                try {
                    block()
                } finally {
                    lock.release()
                }
            }
        }

        private fun ClassLoader.canDriveService(default: Boolean = true): Boolean {
            // 内置的SQLite驱动不支持Mac M1
            if (default && SystemInfo.isMac && /* Mac M1 */ SystemInfo.OS_ARCH.equals("aarch64", ignoreCase = true)) {
                return false
            }

            return try {
                Class.forName(DATABASE_DRIVER, false, this)
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
    }
}