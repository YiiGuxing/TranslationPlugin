@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "ConvertTryFinallyToUseCall", "SqlDialectInspection")

package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.HttpRequests
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.sqlite.SQLiteErrorCode
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Word book service.
 */
class WordBookService {

    @Volatile
    var isInitialized: Boolean = false
        private set

    private val isDownloading: AtomicBoolean = AtomicBoolean()

    private val lockFile: RandomAccessFile
    private lateinit var queryRunner: QueryRunner
    private val settingsPublisher: WordBookListener = Application.messageBus.syncPublisher(WordBookListener.TOPIC)

    init {
        if (!Files.exists(TRANSLATION_DIRECTORY)) {
            Files.createDirectories(TRANSLATION_DIRECTORY)
        }

        lockFile = RandomAccessFile(TRANSLATION_DIRECTORY.resolve(".lock").toFile(), "rw")
        findDriverClassLoader()?.let { initialize(it) }
    }

    private fun initialize(classLoader: ClassLoader) {
        val dataSource = BasicDataSource().apply {
            driverClassLoader = classLoader
            driverClassName = DATABASE_DRIVER
            url = DATABASE_URL
        }
        queryRunner = QueryRunner(dataSource)

        initTable()
    }

    private fun findDriverClassLoader(): ClassLoader? {
        val defaultClassLoader: ClassLoader? = javaClass.classLoader
        if (defaultClassLoader?.canDriveService == true) {
            return defaultClassLoader
        }

        return lock {
            if (!Files.exists(DRIVER_JAR)) {
                return@lock null
            }

            val urlClassLoader = URLClassLoader(arrayOf(DRIVER_JAR.toUri().toURL()), defaultClassLoader)
            return@lock if (urlClassLoader.canDriveService) {
                urlClassLoader
            } else {
                Files.delete(DRIVER_JAR)
                null
            }
        }
    }

    fun downloadDriver(): Boolean {
        if (!isDownloading.compareAndSet(false, true)) {
            return false
        }

        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(null, "Downloading Word Book Driver...", true) {
                override fun run(indicator: ProgressIndicator) = downloadDriverAndInitializeService(indicator)
                override fun onFinished() = isDownloading.set(false)
            })

        return true
    }

    private fun downloadDriverAndInitializeService(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        indicator.text = "Downloading..."

        try {
            AppExecutorUtil.getAppExecutorService().submit {
                indicator.checkCanceled()
                downloadDriverFile(indicator)
            }.get()
        } catch (e: Exception) {
            throw e.cause?.takeIf { it is ProcessCanceledException } ?: e
        }

        indicator.text = "Initializing Service..."
        findDriverClassLoader()?.let { initialize(it) }
    }

    private fun downloadDriverFile(indicator: ProgressIndicator) {
        var downloadedFile: Path? = null
        try {
            val downloaded = HttpRequests.request(DRIVER_FILE_URL)
                .connect(HttpRequests.RequestProcessor { request ->
                    val size = request.connection.contentLength.toLong()
                    val finished = lock {
                        Files.exists(DRIVER_JAR) && Files.size(DRIVER_JAR) == size
                    } ?: false
                    if (finished) {
                        return@RequestProcessor null
                    }

                    val tempFile = Files.createTempFile(TRANSLATION_DIRECTORY, "download.", ".tmp")

                    indicator.isIndeterminate = false
                    request.saveToFile(tempFile.toFile(), indicator)
                    return@RequestProcessor tempFile.takeIf { Files.exists(it) }
                }) ?: return

            downloadedFile = downloaded
            indicator.checkCanceled()

            lock {
                if (Files.exists(DRIVER_JAR) && Files.size(DRIVER_JAR) == Files.size(downloaded)) {
                    return@lock
                }

                Files.move(downloaded, DRIVER_JAR, StandardCopyOption.REPLACE_EXISTING)
            }

            indicator.fraction = 1.0
            indicator.isIndeterminate = true
        } catch (e: IOException) {
            throw  IOException("Failed to download driver file", e)
        } finally {
            downloadedFile?.let { Files.deleteIfExists(it) }
        }
    }

    private fun initTable() {
        lock {
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
            isInitialized = true
            invokeOnDispatchThread { settingsPublisher.onInitialized(this@WordBookService) }
        }
    }

    private inline fun <T> lock(action: () -> T): T? {
        val fileLock = lockFile.channel.lock(0L, Long.MAX_VALUE, true)
        return try {
            action()
        } catch (e: Throwable) {
            if (e is SQLException) {
                LOGGER.e(e.message ?: "", e)
            } else {
                LOGGER.w(e.message ?: "", e)
            }
            null
        } finally {
            fileLock.release()
        }
    }

    /**
     * Test if the specified [word] can be added to the wordbook
     */
    fun canAddToWordbook(word: String?): Boolean {
        return isInitialized && word != null && word.isNotBlank() && word.length <= 60 && '\n' !in word
    }

    private fun checkIsInitialized() = check(isInitialized) { "Word book not initialized" }

    /**
     * Adds the specified word to the word book and returns id if word is inserted.
     */
    fun addWord(item: WordBookItem, notifyOnFailed: Boolean = true): Long? {
        checkIsInitialized()
        return lock {
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
                )?.also {
                    item.id = it
                    invokeOnDispatchThread {
                        settingsPublisher.onWordAdded(this@WordBookService, item)
                    }
                }
            } catch (e: SQLException) {
                if (e.errorCode != SQLiteErrorCode.SQLITE_CONSTRAINT.code) {
                    LOGGER.w("Insert word", e)
                    if (notifyOnFailed) {
                        Notifications.showErrorNotification(
                            null,
                            NOTIFICATION_DISPLAY_ID,
                            message("wordbook.notification.title"),
                            message("wordbook.notification.content.addFailed"),
                            e
                        )
                    }
                }
                null
            }
        }
    }

    fun updateWord(word: WordBookItem): Boolean {
        checkIsInitialized()

        val id = requireNotNull(word.id) { "Required word id was null." }
        val sql =
            "UPDATE wordbook SET $COLUMN_PHONETIC = ?, $COLUMN_EXPLANATION = ?, $COLUMN_TAGS = ? WHERE $COLUMN_ID = ?"
        val updated = lock {
            queryRunner.update(
                sql,
                word.phonetic,
                word.explanation,
                word.tags.joinToString(","),
                id
            ) > 0
        } == true
        if (updated) {
            invokeOnDispatchThread {
                settingsPublisher.onWordUpdated(this@WordBookService, word)
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

        val sql = "UPDATE wordbook SET $COLUMN_TAGS = ? WHERE $COLUMN_ID = ?"
        return lock { queryRunner.update(sql, tags.joinToString(","), id) > 0 } == true
    }

    /**
     * Removes the word by the specified [id].
     */
    fun removeWord(id: Long): Boolean {
        checkIsInitialized()
        return lock {
            queryRunner.update("DELETE FROM wordbook WHERE $COLUMN_ID = $id") > 0
        }?.also { removed ->
            if (removed) {
                invokeOnDispatchThread {
                    settingsPublisher.onWordRemoved(this@WordBookService, id)
                }
            }
        } == true
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

        val sql = """
                SELECT $COLUMN_ID FROM wordbook 
                    WHERE $COLUMN_WORD = ? AND $COLUMN_SOURCE_LANGUAGE = ? AND $COLUMN_TARGET_LANGUAGE = ?
            """.trimIndent()
        return try {
            queryRunner.query(sql, WordIdHandler, wordToQuery, sourceLanguage.code, targetLanguage.code)
        } catch (e: SQLException) {
            LOGGER.e(e.message ?: "", e)
            null
        }
    }

    /**
     * Returns all words.
     */
    fun getWords(): List<WordBookItem> {
        checkIsInitialized()
        return try {
            queryRunner.query("SELECT * FROM wordbook ORDER BY $COLUMN_CREATED_AT DESC", WordListHandler)
        } catch (e: SQLException) {
            LOGGER.e(e.message ?: "", e)
            emptyList()
        }
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
        private const val NOTIFICATION_DISPLAY_ID = "Wordbook"

        private val USER_HOME_PATH = System.getProperty("user.home")
        private val TRANSLATION_DIRECTORY = Paths.get(USER_HOME_PATH, ".translation")

        private const val DRIVER_FILE_URL =
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.28.0/sqlite-jdbc-3.28.0.jar"
        private const val DRIVER_FILE_NAME = "driver.jar"
        private val DRIVER_JAR = TRANSLATION_DIRECTORY.resolve(DRIVER_FILE_NAME)

        private const val DATABASE_DRIVER = "org.sqlite.JDBC"
        private val DATABASE_URL = "jdbc:sqlite:${TRANSLATION_DIRECTORY.resolve("wordbook.db")}"

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
            get() = ServiceManager.getService(WordBookService::class.java)

        private val ClassLoader.canDriveService: Boolean
            get() = try {
                Class.forName(DATABASE_DRIVER, false, this) != null
            } catch (e: Throwable) {
                false
            }

        private fun ResultSet.toWordBookItem(): WordBookItem {
            return WordBookItem(
                getLong(COLUMN_ID),
                getString(COLUMN_WORD),
                Lang.valueOfCode(getString(COLUMN_SOURCE_LANGUAGE)),
                Lang.valueOfCode(getString(COLUMN_TARGET_LANGUAGE)),
                getString(COLUMN_PHONETIC),
                getString(COLUMN_EXPLANATION),
                getString(COLUMN_TAGS),
                getDate(COLUMN_CREATED_AT)
            )
        }
    }
}