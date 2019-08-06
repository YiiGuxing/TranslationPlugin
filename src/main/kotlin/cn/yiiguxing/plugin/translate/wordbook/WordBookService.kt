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
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

/**
 * WordBookService
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
                        "_id"                   INTEGER PRIMARY KEY,
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
        @Suppress("ConvertTryFinallyToUseCall")
        return try {
            action(stat)
        } finally {
            stat.close()
        }
    }

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
                        setString(1, item.word)
                        setString(2, item.sourceLanguage.code)
                        setString(3, item.targetLanguage.code)
                        setString(4, item.phonetic)
                        setString(5, item.explains)
                        setString(6, item.tags.takeIf { it.isNotEmpty() }?.joinToString(","))
                        setDate(7, item.createdAt)
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

    fun getWords(): List<WordBookItem> {
        execute { statement: Statement ->
            @Suppress("SqlNoDataSourceInspection", "SqlResolve")
            val resultSet = statement.executeQuery("SELECT * FROM wordbook;")
            @Suppress("ConvertTryFinallyToUseCall")
            return try {
                generateSequence {
                    if (resultSet.next()) {
                        with(resultSet) {
                            WordBookItem(
                                getString(COLUMN_WORD),
                                Lang.valueOfCode(getString(COLUMN_SOURCE_LANGUAGE)),
                                Lang.valueOfCode(getString(COLUMN_TARGET_LANGUAGE)),
                                getString(COLUMN_PHONETIC),
                                getString(COLUMN_EXPLAINS),
                                getString(COLUMN_TAGS)?.split("".toRegex()) ?: emptyList(),
                                getDate(COLUMN_CREATED_AT)
                            )
                        }
                    } else null
                }.toList()
            } finally {
                resultSet.close()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_DISPLAY_ID = "Wordbook"

        private val USER_HOME_PATH = System.getProperty("user.home")
        private val TRANSLATION_DIRECTORY = Paths.get(USER_HOME_PATH, ".translation")

        private const val DATABASE_DRIVER = "org.sqlite.JDBC"
        private val DATABASE_URL = "jdbc:sqlite:${TRANSLATION_DIRECTORY.resolve("wordbook.db")}"

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