package cn.yiiguxing.plugin.translate.util

private val HEX_DIGITS = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
)

fun ByteArray.toHexString(): String {
    val result = CharArray(size * 2)
    forEachIndexed { index, byte ->
        result[index * 2] = HEX_DIGITS[byte.toInt() ushr 4 and 0xf]
        result[index * 2 + 1] = HEX_DIGITS[byte.toInt() and 0xf]
    }

    return String(result)
}