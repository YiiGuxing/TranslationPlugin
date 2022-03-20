package cn.yiiguxing.plugin.translate.diagnostic.github.issues

import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.UrlBuilder

internal object GitHubIssuesApis {

    private const val API_BASE_URL = "https://api.github.com"
    private const val ISSUES_SEARCH_URL = "$API_BASE_URL/search/issues"
    private const val ACCEPT = "application/vnd.github.v3+json"


    fun search(query: String, page: Int = 1, perPage: Int = 30): IssueSearchResult {
        val url = UrlBuilder(ISSUES_SEARCH_URL)
            .addQueryParameters("q", query)
            .addQueryParameters("page", page.toString())
            .addQueryParameters("per_page", perPage.toString())
            .build()
        return Http.request(url) {
            accept(ACCEPT)
        }
    }

    fun create(repositoryId: String, title: String, body: String, authToken: String): Issue {
        val url = "$API_BASE_URL/repos/$repositoryId/issues"
        return Http.postJson<Issue>(url, mapOf("title" to title, "body" to body)) {
            accept(ACCEPT)
            tuner { it.setRequestProperty("Authorization", authToken) }
        }
    }

}