package cn.yiiguxing.plugin.translate.wordbook

import java.util.*

internal object WordTags {

    private const val TAG_SEPARATOR = ", "
    private val REGEX_TAGS_SEPARATOR = Regex("(\\s*,\\s*)+")
    private val REGEX_WHITESPACE = Regex("\\s{2,}")

    fun getTagsString(tags: Set<String>): String {
        when (tags.size) {
            0 -> return ""
            1 -> return tags.first()
        }
        return when (tags) {
            is SortedSet -> tags
            else -> tags.toSortedSet()
        }.joinToString(TAG_SEPARATOR)
    }

    fun getTagSet(tags: String): Set<String> {
        return getTagSequence(tags)
            ?.toSortedSet()
            ?: emptySet()
    }

    fun toTagSet(tags: String, collection: MutableSet<String>): Set<String> {
        return getTagSequence(tags)
            ?.toCollection(collection)
            ?: collection
    }

    private fun getTagSequence(tags: String): Sequence<String>? {
        return tags
            .trim { it.isWhitespace() || it == ',' }
            .takeIf { it.isNotEmpty() }
            ?.replace(REGEX_WHITESPACE, " ")
            ?.split(REGEX_TAGS_SEPARATOR)
            ?.asSequence()
            ?.filter { it.isNotEmpty() }
    }

}