package cn.yiiguxing.plugin.translate.util

import com.intellij.ide.BrowserUtil

object ReportSubmitter {

    private const val NEW_ISSUE_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new"

    fun submit(message: String, comment: String, stacktrace: String) {
        val body = StringBuilder()
            .appendLine("## Issue Details")
            .appendLine("### Description")
            .appendLine(comment)
            .appendLine()
            .appendLine("### Environment")
            .append("> **Plugin version: ").append(Plugin.version).appendLine("**")
            .appendLine()
            .appendLine(Plugin.ideaInfo)
            .apply {
                if (stacktrace.isNotBlank()) {
                    appendLine()
                    appendLine("### Stack Trace")
                    appendLine("```")
                    appendLine(stacktrace)
                    appendLine("```")
                }
            }

        val reportUrl = UrlBuilder(NEW_ISSUE_URL)
            .addQueryParameter("title", message)
            .addQueryParameter("body", body.toString())
            .build()

        BrowserUtil.browse(reportUrl)
    }

}