package cn.yiiguxing.plugin.translate.util

private val REGEX_WHITESPACE_CHARACTER = Regex("\\s")
private val REGEX_WHITESPACE_CHARACTERS = Regex("\\s+")

fun String.filterIgnore(): String {
    return try {
        Settings.ignoreRegexPattern
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