@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "ConvertTryFinallyToUseCall")

package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.openapi.components.ServiceManager
import org.apache.commons.dbcp2.BasicDataSource
import org.sqlite.SQLiteErrorCode
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.*
import javax.sql.DataSource

/**
 * Wordbook service.
 *
 * Created by Yii.Guxing on 2019/08/05.
 */
class WordBookService {

    private val lockFile: RandomAccessFile
    private val dataSource: DataSource

    init {
        if (!Files.exists(TRANSLATION_DIRECTORY)) {
            Files.createDirectories(TRANSLATION_DIRECTORY)
        }

        lockFile = RandomAccessFile(TRANSLATION_DIRECTORY.resolve(".lock").toFile(), "rw")
        dataSource = BasicDataSource().apply {
            driverClassName = DATABASE_DRIVER
            url = DATABASE_URL
        }

        initTable()
    }

    private fun initTable() {
        lock {
            execute { statement: Statement ->
                val createTableSQL = """
                    CREATE TABLE IF NOT EXISTS wordbook (
                        "$COLUMN_ID"            INTEGER PRIMARY KEY,
                        $COLUMN_WORD            TEXT     NOT NULL,
                        $COLUMN_SOURCE_LANGUAGE TEXT     NOT NULL,
                        $COLUMN_TARGET_LANGUAGE TEXT     NOT NULL,
                        $COLUMN_PHONETIC        TEXT,
                        $COLUMN_EXPLAINS        TEXT,
                        $COLUMN_TAGS            TEXT,
                        $COLUMN_CREATED_AT      DATETIME NOT NULL
                    );
                """.trimIndent()
                statement.executeUpdate(createTableSQL)

                val createIndexSQL = """
                    CREATE UNIQUE INDEX IF NOT EXISTS wordbook_unique_index
                        ON wordbook ($COLUMN_WORD, $COLUMN_SOURCE_LANGUAGE, $COLUMN_TARGET_LANGUAGE);
                """.trimIndent()
                statement.executeUpdate(createIndexSQL)
            }
        }
    }

    private inline fun <T> lock(action: () -> T): T {
        val fileLock = lockFile.channel.lock(0L, Long.MAX_VALUE, true)
        return try {
            action()
        } finally {
            fileLock.release()
        }
    }

    private inline fun <reified T : Statement, R> execute(
        statement: (connection: Connection) -> T = { it.createStatement() as T },
        action: (statement: T) -> R
    ): R {
        val stat = statement(dataSource.connection)
        return try {
            action(stat)
        } finally {
            stat.close()
        }
    }

    /**
     * Test if the specified [word] can be added to the wordbook
     */
    fun canAddToWordbook(word: String?): Boolean {
        return word != null && word.isNotBlank() && word.length <= 60
    }

    /**
     * Adds the specified word to the word book.
     */
    fun addWord(item: WordBookItem) {
        lock {
            val sql = """
                INSERT INTO wordbook (
                    $COLUMN_WORD,
                    $COLUMN_SOURCE_LANGUAGE,
                    $COLUMN_TARGET_LANGUAGE,
                    $COLUMN_PHONETIC,
                    $COLUMN_EXPLAINS,
                    $COLUMN_TAGS,
                    $COLUMN_CREATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()

            try {
                execute({ it.prepareStatement(sql) }) { statement: PreparedStatement ->
                    with(statement) {
                        setWordBookItem(item)
                        executeUpdate()
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
            }
            Unit
        }
    }

    private fun PreparedStatement.setWordBookItem(item: WordBookItem) {
        var index = 0
        setString(++index, item.word)
        setString(++index, item.sourceLanguage.code)
        setString(++index, item.targetLanguage.code)
        setString(++index, item.phonetic)
        setString(++index, item.explains)
        setString(++index, item.tags.takeIf { it.isNotEmpty() }?.joinToString(","))
        setDate(++index, item.createdAt)
    }

    /**
     * Updates the [tags] of the word by the specified [id].
     */
    fun updateTags(id: Long, tags: List<String>): Boolean {
        val sql = "UPDATE wordbook SET $COLUMN_TAGS = '?' WHERE $COLUMN_ID = $id;"
        return lock {
            execute({ it.prepareStatement(sql) }) { statement: PreparedStatement ->
                statement.setString(1, tags.joinToString(","))
                statement.executeUpdate() > 0
            }
        }
    }

    /**
     * Removes the word by the specified [id].
     */
    fun removeWord(id: Long): Boolean {
        return lock {
            execute { statement: Statement ->
                statement.executeUpdate("DELETE FROM wordbook WHERE $COLUMN_ID = $id;") > 0
            }
        }
    }

    /**
     * Returns next random word.
     */
    fun nextWord(): WordBookItem? {
        return execute { statement: Statement ->
            val resultSet = statement.executeQuery("SELECT * FROM wordbook ORDER BY random() LIMIT 1;")
            return try {
                resultSet.takeIf { it.next() }?.toWordBookItem()
            } finally {
                resultSet.close()
            }
        }
    }

    /**
     * Returns the word ID by the specified [word], [source language][sourceLanguage] and [target language][targetLanguage].
     */
    fun getWordId(word: String, sourceLanguage: Lang, targetLanguage: Lang): Int? {
        val sql = """
                SELECT $COLUMN_ID FROM wordbook 
                    WHERE $COLUMN_WORD = '?' AND $COLUMN_SOURCE_LANGUAGE = '?' AND $COLUMN_TARGET_LANGUAGE = '?';
            """.trimIndent()
        return execute({ it.prepareStatement(sql) }) { statement: PreparedStatement ->
            val resultSet = with(statement) {
                var index = 0
                setString(++index, word)
                setString(++index, sourceLanguage.code)
                setString(++index, targetLanguage.code)
                executeQuery()
            }

            return try {
                resultSet.takeIf { it.next() }?.getInt(COLUMN_ID)
            } finally {
                resultSet.close()
            }
        }
    }

    /**
     * Returns all words.
     */
    fun getWords(): List<WordBookItem> {
        execute { statement: Statement ->
            val resultSet = statement.executeQuery("SELECT * FROM wordbook ORDER BY $COLUMN_CREATED_AT DESC;")
            return try {
                generateSequence { resultSet.takeIf { it.next() }?.toWordBookItem() }.toList()
            } finally {
                resultSet.close()
            }
        }
    }

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

        val instance: WordBookService
            get() = ServiceManager.getService(WordBookService::class.java)
    }
}