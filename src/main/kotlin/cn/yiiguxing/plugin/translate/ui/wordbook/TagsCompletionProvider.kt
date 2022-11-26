package cn.yiiguxing.plugin.translate.ui.wordbook

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.ui.TextFieldWithAutoCompletion

internal class TagsCompletionProvider(
    tags: Collection<String>,
    private val onFilterTag: (tag: String) -> Boolean
) : TextFieldWithAutoCompletion.StringsCompletionProvider(tags, null) {

    fun appendTags(tags: Collection<String>) {
        myVariants = myVariants.toMutableSet().apply { addAll(tags) }
    }

    override fun getItems(
        prefix: String?,
        cached: Boolean,
        parameters: CompletionParameters
    ): Collection<String> {
        if (prefix.isNullOrEmpty()) {
            return emptyList()
        }

        val currentTag = getTagAtCurrentOffset(parameters)
        val items = myVariants.filterTo(ArrayList(myVariants.size)) { tag ->
            !tag.isNullOrEmpty() && (tag == currentTag || onFilterTag(tag))
        }
        return items.sortedWith(this)
    }

    override fun getPrefix(text: String, offset: Int): String {
        val i = text.lastIndexOf(TAG_SEPARATOR, offset - 1) + 1
        return text.substring(i, offset).trimStart()
    }

    override fun acceptChar(c: Char): CharFilter.Result? {
        return when (c) {
            TAG_SEPARATOR -> CharFilter.Result.HIDE_LOOKUP
            else -> null
        }
    }

    companion object {
        private const val TAG_SEPARATOR = ','

        /**
         * Returns the tag at the current cursor offset.
         *
         * ```
         *   "Tag1, Ta|g2, Tag3, ..."
         *            ^ the cursor
         *   ==> Tag2
         * ```
         */
        private fun getTagAtCurrentOffset(parameters: CompletionParameters): String? {
            val text = parameters.originalFile.text
            val offset = parameters.offset
            /*
             *  "Tag1, Ta|g2, Tag3, ..."
             *        ^  ^  ^
             *        i  |  j
             *           the cursor
             *  ==> " Tag2" => "Tag2"
             */
            val i = text.lastIndexOf(TAG_SEPARATOR, offset - 1) + 1
            var j = text.indexOf(TAG_SEPARATOR, offset)
            if (j < i) {
                j = text.length
            }

            return if (j > i) {
                text.substring(i, j).trim().takeIf { it.isNotEmpty() }
            } else null
        }
    }

}