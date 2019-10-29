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

    fun addQueryParameters(name: String, vararg values: String) = apply {
        queryParameters += values.map { name to it }
    }

    fun addQueryParameters(vararg parameters: Pair<String, String>) = apply {
        queryParameters += parameters
    }

    fun build(): String {
        if (queryParameters.isEmpty()) {
            return baseUrl
        }

        return StringBuilder(baseUrl).apply {
            queryParameters.forEachIndexed { index, (name, value) ->
                append(if (index == 0) "?" else "&")
                append(name, "=", value.urlEncode())
            }
        }.toString()
    }

}