package cn.yiiguxing.plugin.translate.util

@Suppress("unused")
/**
 * UrlBuilder
 */
class UrlBuilder(private val baseUrl: String) {

    private val queryParameters = mutableListOf<Pair<String, String>>()

    fun addQueryParameter(name: String, value: String) = apply {
        queryParameters += name to value
    }

    fun addQueryParameters(name: String, value: String, vararg other: String) = apply {
        queryParameters += name to value
        queryParameters += other.map { name to it }
    }

    fun addQueryParameters(parameter: Pair<String, String>, vararg other: Pair<String, String>) = apply {
        queryParameters += parameter
        queryParameters += other
    }

    fun build(): String {
        if (queryParameters.isEmpty()) {
            return baseUrl
        }

        val base = baseUrl.trimEnd('&', '?')
        val hasParams = base.contains('?')
        return StringBuilder(base).apply {
            queryParameters.forEachIndexed { index, (name, value) ->
                append(if (index == 0 && !hasParams) "?" else "&")
                append(name.urlEncode(), "=", value.urlEncode())
            }
        }.toString()
    }

}