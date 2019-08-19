@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "ConvertTryFinallyToUseCall", "SqlDialectInspection")

package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeOnDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.sqlite.SQLiteErrorCode
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Word book service.
 *
 * Created by Yii.Guxing on 2019/08/05.
 */
class WordBookService {

    private val lockFile: RandomAccessFile
    private val queryRunner: QueryRunner
    private val settingsChangePublisher: WordBookChangeListener =
        ApplicationManager.getApplication().messageBus.syncPublisher(WordBookChangeListener.TOPIC)

    init {
        if (!Files.exists(TRANSLATION_DIRECTORY)) {
            Files.createDirectories(TRANSLATION_DIRECTORY)
        }

        lockFile = RandomAccessFile(TRANSLATION_DIRECTORY.resolve(".lock").toFile(), "rw")

        val dataSource = BasicDataSource().apply {
            driverClassName = DATABASE_DRIVER
            url = DATABASE_URL
        }
        queryRunner = QueryRunner(dataSource)

        executeOnPooledThread { initTable() }
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
                        $COLUMN_EXPLAINS        TEXT,
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
    }

    private inline fun <T> lock(action: () -> T): T? {
        val fileLock = lockFile.channel.lock(0L, Long.MAX_VALUE, true)
        return try {
            action()
        } catch (e: Throwable) {
            LOGGER.e(e.message ?: "", e)
            null
        } finally {
            fileLock.release()
        }
    }

    /**
     * Test if the specified [word] can be added to the wordbook
     */
    fun canAddToWordbook(word: String?): Boolean {
        return word != null && word.isNotBlank() && word.length <= 60 && '\n' !in word
    }

    /**
     * Adds the specified word to the word book and returns id if word is inserted.
     */
    fun addWord(item: WordBookItem): Long? {
        return lock {
            val sql = """
                INSERT INTO wordbook (
                    $COLUMN_WORD,
                    $COLUMN_SOURCE_LANGUAGE,
                    $COLUMN_TARGET_LANGUAGE,
                    $COLUMN_PHONETIC,
                    $COLUMN_EXPLAINS,
                    $COLUMN_TAGS,
                    $COLUMN_CREATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            return try {
                queryRunner.insert<Long?>(
                    sql,
                    WordIdHandler,
                    item.word,
                    item.sourceLanguage.code,
                    item.targetLanguage.code,
                    item.phonetic,
                    item.explains,
                    item.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                    item.createdAt
                )?.also {
                    item.id = it
                    invokeOnDispatchThread {
                        settingsChangePublisher.onWordAdded(this@WordBookService, item)
                    }
                }
            } catch (e: SQLException) {
                if (e.errorCode != SQLiteErrorCode.SQLITE_CONSTRAINT.code) {
                    @Suppress("InvalidBundleOrProperty")
                    Notifications.showErrorNotification(
                        null,
                        NOTIFICATION_DISPLAY_ID,
                        message("notification.title.wordbook"),
                        message("notification.content.wordbook.addFailed"),
                        e
                    )
                }
                null
            }
        }
    }

    /**
     * Updates the [tags] of the word by the specified [id].
     */
    fun updateTags(id: Long, tags: List<String>): Boolean {
        val sql = "UPDATE wordbook SET $COLUMN_TAGS = ? WHERE $COLUMN_ID = ?"
        return lock { queryRunner.update(sql, tags.joinToString(","), id) > 0 } == true
    }

    /**
     * Removes the word by the specified [id].
     */
    fun removeWord(id: Long): Boolean {
        return lock {
            queryRunner.update("DELETE FROM wordbook WHERE $COLUMN_ID = $id") > 0
        }?.also { removed ->
            if (removed) {
                invokeOnDispatchThread {
                    settingsChangePublisher.onWordRemoved(this@WordBookService, id)
                }
            }
        } == true
    }

    /**
     * Returns next random word.
     */
    fun nextWord(): WordBookItem? {
        return try {
            queryRunner.query("SELECT * FROM wordbook ORDER BY random() LIMIT 1", WordHandler)
        } catch (e: SQLException) {
            LOGGER.e(e.message ?: "", e)
            null
        }
    }

    /**
     * Returns the word ID by the specified [word], [source language][sourceLanguage] and [target language][targetLanguage].
     */
    fun getWordId(word: String, sourceLanguage: Lang, targetLanguage: Lang): Long? {
        val sql = """
                SELECT $COLUMN_ID FROM wordbook 
                    WHERE $COLUMN_WORD = ? AND $COLUMN_SOURCE_LANGUAGE = ? AND $COLUMN_TARGET_LANGUAGE = ?
            """.trimIndent()
        return try {
            queryRunner.query(sql, WordIdHandler, word, sourceLanguage.code, targetLanguage.code)
        } catch (e: SQLException) {
            LOGGER.e(e.message ?: "", e)
            null
        }
    }

    /**
     * Returns all words.
     */
    fun getWords(): List<WordBookItem> {
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

    private object WordHandler : ResultSetHandler<WordBookItem?> {
        override fun handle(rs: ResultSet): WordBookItem? = rs.takeIf { it.next() }?.toWordBookItem()
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

        private const val DATABASE_DRIVER = "org.sqlite.JDBC"
        private val DATABASE_URL = "jdbc:sqlite:${TRANSLATION_DIRECTORY.resolve("wordbook.db")}"

        private const val COLUMN_ID = "_id"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_SOURCE_LANGUAGE = "source_language"
        private const val COLUMN_TARGET_LANGUAGE = "target_language"
        private const val COLUMN_PHONETIC = "phonetic"
        private const val COLUMN_EXPLAINS = "explains"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_CREATED_AT = "created_at"

        private val LOGGER = Logger.getInstance(WordBookService::class.java)

        val instance: WordBookService
            get() = ServiceManager.getService(WordBookService::class.java)

        private fun ResultSet.toWordBookItem(): WordBookItem {
            return WordBookItem(
                getLong(COLUMN_ID),
                getString(COLUMN_WORD),
                Lang.valueOfCode(getString(COLUMN_SOURCE_LANGUAGE)),
                Lang.valueOfCode(getString(COLUMN_TARGET_LANGUAGE)),
                getString(COLUMN_PHONETIC),
                getString(COLUMN_EXPLAINS),
                getString(COLUMN_TAGS)?.split(",") ?: emptyList(),
                getDate(COLUMN_CREATED_AT)
            )
        }
    }
}