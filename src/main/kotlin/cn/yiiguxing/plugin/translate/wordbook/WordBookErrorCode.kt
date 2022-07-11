package cn.yiiguxing.plugin.translate.wordbook

/**
 * Wordbook error code.
 *
 * See Also: org.sqlite.SQLiteErrorCode
 */
enum class WordBookErrorCode(
    /** org.sqlite.SQLiteErrorCode.code */
    val code: Int,
    val reason: String
) {
    UNKNOWN_ERROR(-1, "UNKNOWN_ERROR"),

    // SQLiteErrorCode.SQLITE_ABORT
    ABORTED(4, "ABORTED"),

    // SQLiteErrorCode.SQLITE_BUSY
    BUSY(5, "BUSY"),

    // SQLiteErrorCode.SQLITE_LOCKED
    LOCKED(6, "LOCKED"),

    // SQLiteErrorCode.SQLITE_INTERRUPT
    INTERRUPTED(9, "INTERRUPTED"),

    // SQLiteErrorCode.SQLITE_IOERR
    IO_ERROR(10, "IO_ERROR"),

    // SQLiteErrorCode.SQLITE_CORRUPT
    CORRUPT(11, "CORRUPT"),

    // SQLiteErrorCode.SQLITE_FULL
    FULL(13, "FULL"),

    // SQLiteErrorCode.SQLITE_CANTOPEN
    CANT_OPEN(14, "CANT_OPEN"),

    // SQLiteErrorCode.SQLITE_CONSTRAINT
    CONSTRAINT(19, "CONSTRAINT"),

    // SQLiteErrorCode.SQLITE_NOTADB
    NOT_A_DB(26, "NOT_A_DB");

    override fun toString(): String {
        return "[$name][$reason]"
    }

    companion object {

        operator fun get(code: Int): WordBookErrorCode = when (code) {
            ABORTED.code -> ABORTED
            BUSY.code -> BUSY
            LOCKED.code -> LOCKED
            INTERRUPTED.code -> INTERRUPTED
            IO_ERROR.code -> IO_ERROR
            CORRUPT.code -> CORRUPT
            FULL.code -> FULL
            CANT_OPEN.code -> CANT_OPEN
            CONSTRAINT.code -> CONSTRAINT
            NOT_A_DB.code -> NOT_A_DB
            else -> UNKNOWN_ERROR
        }

    }
}