/*
 * Strings
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import java.net.URLEncoder
import java.security.MessageDigest

fun String?.isNullOrBlank() = (this as CharSequence?).isNullOrBlank()


private val REGEX_UNDERLINE = Regex("([A-Za-z])_+([A-Za-z])")
private val REGEX_NUM_WORD = Regex("([0-9])([A-Za-z])")
private val REGEX_WORD_NUM = Regex("([A-Za-z])([0-9])")
private val REGEX_LOWER_UPPER = Regex("([a-z])([A-Z])")
private val REGEX_UPPER_WORD = Regex("([A-Z])([A-Z][a-z])")
private val REGEX_WHITESPACE_CHARACTER = Regex("\\s")
private val REGEX_WHITESPACE_CHARACTERS = Regex("\\s+")
private const val REPLACEMENT_SPLIT_GROUP = "$1 $2"

/**
 * 单词拆分
 */
fun String.splitWords(): String {
    return replace(REGEX_UNDERLINE, REPLACEMENT_SPLIT_GROUP)
        .replace(REGEX_NUM_WORD, REPLACEMENT_SPLIT_GROUP)
        .replace(REGEX_WORD_NUM, REPLACEMENT_SPLIT_GROUP)
        .replace(REGEX_LOWER_UPPER, REPLACEMENT_SPLIT_GROUP)
        .replace(REGEX_UPPER_WORD, REPLACEMENT_SPLIT_GROUP)
}

fun String.filterIgnore(): String {
    return try {
        Settings.ignoreRegExp
            ?.takeIf { it.isNotEmpty() }
            ?.toRegex(RegexOption.MULTILINE)
            ?.let { replace(it, "") }
            ?: this
    } catch (e: Exception) {
        this
    }
}

fun String.processBeforeTranslate(): String? {
    val filteredIgnore = filterIgnore()
    val formatted = if (!Settings.keepFormat) {
        filteredIgnore.replace(REGEX_WHITESPACE_CHARACTERS, " ").trim()
    } else filteredIgnore

    return formatted
        .takeIf { it.isNotBlank() }
        ?.let { if (!it.contains(REGEX_WHITESPACE_CHARACTER)) it.splitWords() else it }
}

/**
 * 分割句子
 *
 * @param maxSentenceLength 句子最大长度
 * @throws IllegalArgumentException 如果[maxSentenceLength] <= 0.
 *
 * @see String.splitSentenceTo
 */
fun String.splitSentence(maxSentenceLength: Int): List<String> = when {
    maxSentenceLength <= 0 -> throw IllegalArgumentException("maxSentenceLength must be greater than 0.")
    isBlank() -> emptyList()
    else -> splitSentenceTo(ArrayList(), maxSentenceLength)
}

/**
 * 分割句子到指定集合
 *
 * @param destination       目标集合
 * @param maxSentenceLength 句子最大长度
 * @throws IllegalArgumentException 如果[maxSentenceLength] <= 0.
 */
fun <C : MutableCollection<String>> String.splitSentenceTo(destination: C, maxSentenceLength: Int): C {
    require(maxSentenceLength > 0) { "maxSentenceLength must be greater than 0." }

    if (isBlank()) {
        return destination
    }

    val whitespaceReg = Regex("[ \\u3000\\n\\r\\t\\s]+") // \u3000:全角空格
    val optimized = replace(whitespaceReg, " ")

    if (optimized.length <= maxSentenceLength) {
        destination += optimized
        return destination
    }

    return optimized.splitSentenceTo(destination, maxSentenceLength, String::splitByPunctuation) {
        splitSentenceTo(destination, maxSentenceLength, String::splitBySpace) {
            splitByLengthTo(destination, maxSentenceLength)
        }
    }
}

private fun <C : MutableCollection<String>> String.splitSentenceTo(
    destination: C,
    maxSentenceLength: Int,
    splitFun: String.() -> List<String>,
    reSplitFun: String.(C) -> Unit
): C {
    val sentences = splitFun()
    val sentenceBuilder = StringBuilder()

    for (sentence in sentences) {
        val merged = (sentenceBuilder.toString() + sentence).trim()
        if (merged.length <= maxSentenceLength) {
            sentenceBuilder.append(sentence)
        } else {
            if (sentenceBuilder.isNotBlank()) {
                destination += sentenceBuilder.trim().toString()
                sentenceBuilder.setLength(0)
            }

            val trimmedSentence = sentence.trim()
            if (trimmedSentence.length <= maxSentenceLength) {
                sentenceBuilder.setLength(0)
                sentenceBuilder.append(sentence)
            } else {
                trimmedSentence.reSplitFun(destination)
            }
        }
    }

    if (sentenceBuilder.isNotBlank()) {
        destination += sentenceBuilder.trim().toString()
    }

    return destination
}

private fun String.splitByPunctuation() = splitBy(Regex("([?.,;:!][ ]+)|([、。！（），．：；？][ ]?)"))
private fun String.splitBySpace() = splitBy(Regex(" "))

private fun String.splitBy(regex: Regex): List<String> {
    val splits = mutableListOf<String>()
    var currIndex = 0
    for (mr in regex.findAll(this)) {
        val index = mr.range.last + 1
        if (index > currIndex) {
            splits += substring(currIndex, index)
        }
        currIndex = index
    }
    if (length > currIndex) {
        splits += substring(currIndex)
    }

    return splits
}

private fun String.splitByLengthTo(destination: MutableCollection<String>, maxLength: Int) {
    for (i in 0 until length step maxLength) {
        destination += substring(i, minOf(i + maxLength, length))
    }
}

/**
 * URL编码
 */
fun String.urlEncode(): String = if (isEmpty()) this else URLEncoder.encode(this, "UTF-8")

private val hexDigits = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
)

fun String.getMessageDigest(algorithm: String): String {
    val messageDigest = with(MessageDigest.getInstance(algorithm)) {
        update(toByteArray(Charsets.UTF_8))
        digest()
    }

    val result = CharArray(messageDigest.size * 2)
    messageDigest.forEachIndexed { index, byte ->
        result[index * 2] = hexDigits[byte.toInt() ushr 4 and 0xf]
        result[index * 2 + 1] = hexDigits[byte.toInt() and 0xf]
    }

    return String(result)
}

/**
 * 生成32位MD5摘要
 * @return MD5摘要
 */
fun String.md5(): String = getMessageDigest("MD5")

/**
 * 生成SHA-256摘要
 * @return SHA-256摘要
 */
fun String.sha256(): String = getMessageDigest("SHA-256")
