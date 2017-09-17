/*
 * Strings
 * 
 * Created by Yii.Guxing on 2017/9/11
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import java.net.URLEncoder
import java.security.MessageDigest

private fun toUpperCase(a: Char): Char = when {
    a < 'a' -> a
    a <= 'z' -> (a.toInt() + ('A' - 'a')).toChar()
    else -> Character.toUpperCase(a)
}

/**
 * Capitalize the first letter of the sentence.
 */
fun String.capitalize(): String = when {
    isEmpty() -> this
    length == 1 -> toUpperCase(this[0]).toString()
    Character.isUpperCase(this[0]) -> this
    else -> toUpperCase(this[0]) + substring(1)
}

/**
 * 单词拆分
 */
fun String.splitWord() = if (isBlank()) this else
    replace("[_*\\s]+".toRegex(), " ")
            .replace("([A-Z][a-z]+)|([0-9\\W]+)".toRegex(), " $0 ")
            .replace("[A-Z]{2,}".toRegex(), " $0")
            .replace("\\s{2,}".toRegex(), " ")
            .trim()

/**
 * URL编码
 */
fun String.urlEncode(): String = if (isEmpty()) this else URLEncoder.encode(this, "UTF-8")

private val HEX_DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
)

/**
 * 生成32位MD5摘要
 * @return MD5摘要
 */
fun String.md5(): String {
    val md5Digest = with(MessageDigest.getInstance("MD5")) {
        update(toByteArray(Charsets.UTF_8))
        digest()
    }

    val result = CharArray(md5Digest.size * 2)
    md5Digest.forEachIndexed { index, byte ->
        result[index * 2] = HEX_DIGITS[byte.toInt() ushr 4 and 0xf]
        result[index * 2 + 1] = HEX_DIGITS[byte.toInt() and 0xf]
    }

    return String(result)
}
