package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import com.intellij.util.io.RequestBuilder
import com.intellij.xml.util.XmlStringUtil
import java.awt.Component
import java.text.DateFormat
import java.util.*

class ReportSubmitter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = adaptedMessage("error.report.to.yiiguxing.action")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val event = events[0]
        val message = event.message
        val stacktrace = event.throwableText
        if (stacktrace.isNullOrBlank()) {
            return false
        }

        val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent))
        object : Task.Backgroundable(project, message("title.submitting.error.report"), false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    doSubmit(project, message, stacktrace, additionalInfo, consumer)
                } catch (e: Exception) {
                    onError(project, e, events, additionalInfo, parentComponent, consumer)
                }
            }
        }.queue()

        return true
    }

    private fun doSubmit(
        project: Project?,
        message: String?,
        stacktrace: String,
        additionalInfo: String?,
        consumer: Consumer<in SubmittedReportInfo>
    ) {
        val issueSHA = stacktrace.md5()
        val (status, issue) = findIssue(issueSHA)
            ?.let { issue -> SubmittedReportInfo.SubmissionStatus.DUPLICATE to issue }
            ?: postNewIssue(issueSHA, message, additionalInfo, stacktrace)
                .let { issue -> SubmittedReportInfo.SubmissionStatus.NEW_ISSUE to issue }

        val reportInfo = SubmittedReportInfo(issue.htmlUrl, "Issue#${issue.number}", status)
        invokeLater {
            consumer.consume(reportInfo)
            showIssueNotification(project, reportInfo)
        }
    }

    private fun showIssueNotification(project: Project?, reportInfo: SubmittedReportInfo) {
        val text = StringBuilder().apply {
            append(message("error.report.submitted.as.link", reportInfo.url, reportInfo.linkText))
            if (reportInfo.status == SubmittedReportInfo.SubmissionStatus.DUPLICATE) {
                append(message("error.report.duplicate"))
            }
            append('.')
            append("<br/>")
            append(message("error.report.gratitude"))
        }
        val content = XmlStringUtil.wrapInHtml(text)
        val title = message("error.report.submitted")
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Error Report")
            .createNotification(title, content, NotificationType.INFORMATION)
            .setListener(NotificationListener.URL_OPENING_LISTENER)
            .setImportant(false)
            .notify(project)
    }

    private fun onError(
        project: Project?,
        e: Exception,
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        callback: Consumer<in SubmittedReportInfo>
    ) {
        logger.w("reporting failed:", e)
        invokeLater {
            val message = message("error.report.failed.message", e.message.toString())
            val title = message("error.report.failed.title")
            val result = MessageDialogBuilder.yesNo(title, message).ask(project)
            if (!result || !submit(events, additionalInfo, parentComponent, callback)) {
                callback.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
            }
        }
    }

    private fun findIssue(issueSHA: String): Issue? {
        val url = "$ISSUES_SEARCH_URL+$issueSHA"
        val result = Http.request<IssueSearchResult>(url) { acceptGitHubV3Json() }
        val issue = result.items.firstOrNull()
        if (issue != null) {
            logger.d("Issue is actually a duplicate of existing one: $result")
        }

        return issue
    }

    private fun postNewIssue(issueSHA: String, message: String?, comment: String?, stacktrace: String): Issue {
        val eventMessage = message?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
        val title = "[Auto Generated]Plugin error occurred$eventMessage"
        val body = StringBuilder()
            .appendLine(":warning:_`[Auto Generated Report]-=$issueSHA=-`_\n")
            .appendLine("## Description")
            .appendLine(comment ?: "")
            .appendLine()
            .appendEnvironments()
            .apply {
                if (stacktrace.isNotBlank()) {
                    appendLine()
                    appendLine("## Stack Trace")
                    appendLine("```")
                    appendLine(stacktrace)
                    appendLine("```")
                }
            }
            .toString()

        return Http.postJson<Issue>(NEW_ISSUE_POST_URL, mapOf("title" to title, "body" to body)) {
            acceptGitHubV3Json()
            tuner { it.setRequestProperty("Authorization", "token $ACCESS_TOKEN") }
        }
    }

    private fun StringBuilder.appendEnvironments() = apply {
        appendLine("## Environments")
        append("> **Plugin version: ", Plugin.version, "**", "\n\n")

        val appInfo = ApplicationInfoEx.getInstanceEx()
        val edition = ApplicationNamesInfo.getInstance().editionName
        append(appInfo.fullApplicationName, edition?.let { " ($it)" } ?: "", "\n")

        append(
            "Build #",
            appInfo.build.asString(),
            ", built on ",
            DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(appInfo.buildDate.time),
            "\n"
        )

        val properties = System.getProperties()
        val javaVersion: String =
            properties.getProperty("java.runtime.version", properties.getProperty("java.version", "unknown"))
        val arch: String = properties.getProperty("os.arch", "")
        append("Runtime version: ", javaVersion, " ", arch, "\n")

        val vmVersion = properties.getProperty("java.vm.name", "unknown")
        val vmVendor = properties.getProperty("java.vendor", "unknown")
        append("VM: ", vmVersion, " by ", vmVendor, "\n")
        append("Operating system: ", SystemInfo.getOsNameAndVersion(), "\n")
        append("Last action id: ", IdeaLogger.ourLastActionId, "\n")
    }


    internal data class Issue(val number: Int, @SerializedName("html_url") val htmlUrl: String)

    internal data class IssueSearchResult(val items: List<Issue>)


    companion object {
        private const val API_BASE_URL = "https://api.github.com"
        private const val REPO = "YiiGuxing/TranslationPlugin"
        @Suppress("SpellCheckingInspection")
        private const val ACCESS_TOKEN = "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"
        private const val NEW_ISSUE_POST_URL = "$API_BASE_URL/repos/$REPO/issues"
        private const val ISSUES_SEARCH_URL = "$API_BASE_URL/search/issues?per_page=1&q=repo:$REPO+is:issue+in:body"
        private const val NEW_ISSUE_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new"

        private val logger = Logger.getInstance(ReportSubmitter::class.java)

        private fun RequestBuilder.acceptGitHubV3Json() = accept("application/vnd.github.v3+json")

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

}