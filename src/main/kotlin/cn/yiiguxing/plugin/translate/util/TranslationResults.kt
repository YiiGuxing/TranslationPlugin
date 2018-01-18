package cn.yiiguxing.plugin.translate.util

import java.util.*
import java.util.regex.Pattern

/**
 * TranslationResults
 * <p>
 * Created by Yii.Guxing on 2017-09-17 0017.
 */
@Suppress("unused")
object TranslationResults {

    private val PATTERN_EXPLAIN = Pattern.compile(
            "(^(a|adj|prep|pron|n|v|conj|s|sc|o|oc|vi|vt|aux|ad|adv|art|num|int|u|c|pl|abbr)\\.)(.+)"
    )
    private const val GROUP_CLASS = 1
    private const val GROUP_EXPLAIN = 3

    /**
     * 拆分单词解释。例如：将 "vt. 分离; 使分离" 拆分为 {"vt.", "分离; 使分离"}
     */
    fun splitExplain(input: String): Pair<String?, String> {
        val explainMatcher = PATTERN_EXPLAIN.matcher(input)
        return if (explainMatcher.find()) {
            explainMatcher.group(GROUP_CLASS) to explainMatcher.group(GROUP_EXPLAIN).trim()
        } else {
            null to input
        }
    }

    /**
     * 展开像 'Hello; Hi' 这样的解释
     */
    fun expandExplain(explains: Array<String>?): Array<String>? {
        if (explains == null || explains.isEmpty())
            return explains

        val result = LinkedHashSet<String>(explains.size)
        val pattern = Pattern.compile("[;；]")
        for (explain in explains) {
            Collections.addAll(result, *pattern.split(explain))
        }

        return result.toTypedArray()
    }

}