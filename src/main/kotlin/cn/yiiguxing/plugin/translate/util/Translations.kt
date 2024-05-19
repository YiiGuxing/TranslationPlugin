package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.Settings

private val REGEX_WHITESPACE_CHARACTERS = Regex("\\s+")
private val REGEX_WORDS = Regex("^\\w{2,}$")

fun String.filterIgnore(): String {
    return try {
        Settings.getInstance().ignoreRegexPattern
            ?.let { replace(it, "") }
            ?: this
    } catch (e: Exception) {
        this
    }
}

fun String.processBeforeTranslate(): String? {
    val filteredIgnore = filterIgnore()
    val formatted = if (!Settings.getInstance().keepFormat) {
        filteredIgnore.replace(REGEX_WHITESPACE_CHARACTERS, " ").trim()
    } else filteredIgnore

    return formatted
        .takeIf { it.isNotBlank() }
        ?.splitCamelCaseWords()
}

/**
 * Splits camel case words.
 */
fun String.splitCamelCaseWords(): String = when {
    matches(REGEX_WORDS) -> CamelCaseSplitter.split(this)
        .filter { it[0] != '_' }.let { words ->
            when {
                words.size == 1 -> words[0]
                else -> words.joinToString(" ") {
                    if (it.all { c -> c.isUpperCase() }) it else it.lowercase()
                }
            }
        }

    else -> this
}