package cn.yiiguxing.plugin.translate.util


/**
 * 根据指定的[正则表达式][regex]将字符序列分块，分为匹配和未匹配目标正则表达式的块。
 */
fun CharSequence.chunked(regex: Regex, transform: (MatchResult) -> CharSequence): List<CharSequence> {
    val chunked = mutableListOf<CharSequence>()

    var cursor = 0
    for (matchResult in regex.findAll(this)) {
        val start = matchResult.range.first
        if (start != 0) {
            chunked += substring(cursor, matchResult.range.first)
        }
        chunked += transform(matchResult)
        cursor = matchResult.range.last + 1
    }

    if (cursor < length) {
        chunked += substring(cursor, length)
    }

    return chunked
}