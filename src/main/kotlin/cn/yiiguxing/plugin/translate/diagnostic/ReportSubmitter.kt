package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.annotations.SerializedName
import com.intellij.credentialStore.Credentials
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
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
import javax.swing.JComponent

class ReportSubmitter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = adaptedMessage("error.report.to.yiiguxing.action")

    override fun getReporterAccount(): String = ReportCredentials.userName

    override fun changeReporterAccount(parentComponent: Component) {
        val project = parentComponent.getProject()

        if (!ReportCredentials.isAnonymous) {
            val title = message("error.change.reporter.account.title")
            val message = message("error.change.reporter.account.message")
            if (!MessageDialogBuilder.yesNo(title, message).ask(project)) {
                return
            }
        }

        ReportCredentials.requestNewCredentials(project, parentComponent as JComponent)
    }


    private fun Component.getProject(): Project? {
        val dataContext = DataManager.getInstance().getDataContext(this)
        return CommonDataKeys.PROJECT.getData(dataContext)
    }

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

        val credentials = ReportCredentials.credentials

        val project = parentComponent.getProject()
        object : Task.Backgroundable(project, message("title.submitting.error.report"), false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    doSubmit(project, credentials, message, stacktrace, additionalInfo, consumer)
                } catch (e: Exception) {
                    onError(project, e, events, additionalInfo, parentComponent, consumer)
                }
            }
        }.queue()

        return true
    }

    private fun doSubmit(
        project: Project?,
        credentials: Credentials,
        message: String?,
        stacktrace: String,
        additionalInfo: String?,
        consumer: Consumer<in SubmittedReportInfo>
    ) {
        val issueID = stacktrace.md5()
        val (status, issue) = findIssue(issueID)
            ?.let { issue -> SubmittedReportInfo.SubmissionStatus.DUPLICATE to issue }
            ?: postNewIssue(credentials, issueID, message, additionalInfo, stacktrace)
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

        val title = message("error.report.submitted")
        val content = XmlStringUtil.wrapInHtml(text)
        ErrorReportNotifications.showNotification(project, title, content)
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

    private fun findIssue(issueId: String): Issue? {
        val url = "$ISSUES_SEARCH_URL+$issueId"
        val result = Http.request<IssueSearchResult>(url) { acceptGitHubV3Json() }
        val issue = result.items.firstOrNull()
        if (issue != null) {
            logger.d("Issue is actually a duplicate of existing one: $result")
        }

        return issue
    }

    private fun postNewIssue(
        credentials: Credentials,
        issueId: String,
        message: String?,
        comment: String?,
        stacktrace: String
    ): Issue {
        val eventMessage = message?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
        val title = "[Auto Generated]Plugin error occurred$eventMessage"
        val body = StringBuilder()
            .appendLine(":warning:_`[Auto Generated Report]-=$issueId=-`_")
            .appendLine("<!-- Auto Generated Report. DO NOT MODIFY!!! -->\n")
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
            tuner { it.setRequestProperty("Authorization", credentials.getPasswordAsString()) }
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
        private const val NEW_ISSUE_POST_URL = "$API_BASE_URL/repos/$REPO/issues"
        private const val ISSUES_SEARCH_URL = "$API_BASE_URL/search/issues?per_page=1&q=repo:$REPO+is:issue+in:body"

        private val logger = Logger.getInstance(ReportSubmitter::class.java)

        private fun RequestBuilder.acceptGitHubV3Json() = accept("application/vnd.github.v3+json")
    }

}