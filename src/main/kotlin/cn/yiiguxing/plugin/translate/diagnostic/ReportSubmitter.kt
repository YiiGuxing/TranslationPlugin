package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.action.BrowseUrlAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.diagnostic.github.TranslationGitHubAppException
import cn.yiiguxing.plugin.translate.diagnostic.github.TranslationGitHubAppService
import cn.yiiguxing.plugin.translate.diagnostic.github.issues.GitHubIssuesApis
import cn.yiiguxing.plugin.translate.diagnostic.github.issues.Issue
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.credentialStore.Credentials
import com.intellij.diagnostic.AbstractMessage
import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Consumer
import com.intellij.util.io.HttpRequests
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil
import io.netty.handler.codec.http.HttpResponseStatus
import java.awt.Component
import java.text.DateFormat
import java.util.*
import javax.swing.JComponent

internal class ReportSubmitter : ErrorReportSubmitter() {

    companion object {
        private const val TARGET_REPOSITORY = "YiiGuxing/TranslationPlugin"

        private const val EXCEPTION_CLASS_CHANGED_MESSAGE = "*** exception class was changed or removed"

        private val LOG = Logger.getInstance(ReportSubmitter::class.java)
    }

    override fun getReportActionText(): String = adaptedMessage("error.report.to.yiiguxing.action")

    override fun getPrivacyNoticeText(): String = adaptedMessage("error.report.notice")

    override fun getReporterAccount(): String = ReportCredentials.instance.userName

    override fun changeReporterAccount(parentComponent: Component) {
        val project = parentComponent.getProject()

        val reportCredentials = ReportCredentials.instance
        if (!reportCredentials.isAnonymous) {
            val title = message("error.change.reporter.account.title")
            val message = message("error.change.reporter.account.message")

            val choice = MessageDialogBuilder.yesNoCancel(title, message)
                .noText(adaptedMessage("error.change.reporter.account.anonymous.button"))
                .show(project)

            when (choice) {
                MessageConstants.YES -> reportCredentials.clear()
                MessageConstants.NO -> {
                    // Use anonymous account
                    reportCredentials.clear()
                    return
                }
                else -> return
            }
        }

        requestNewCredentials(project, parentComponent as? JComponent)
    }

    private fun requestNewCredentials(project: Project?, parentComponent: JComponent?) {
        val (user, token) = try {
            TranslationGitHubAppService.instance.auth(project, parentComponent as JComponent) ?: return
        } catch (e: Exception) {
            LOG.w("Failed to request new credentials", e)

            val title = message("error.change.reporter.account.failed.title")
            val message = if (e is TranslationGitHubAppException) {
                e.message
            } else {
                message("error.change.reporter.account.failed.message", e.message.toString())
            }
            ErrorReportNotifications.showNotification(
                project,
                title,
                message,
                notificationType = NotificationType.ERROR
            )
            return
        }
        ReportCredentials.instance.save(user.userName, token.authorizationToken)
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
        val message = event.getUsefulMessage()
        val stacktrace = event.stacktrace
        if (stacktrace.isEmpty()) {
            return false
        }

        val credentials = ReportCredentials.instance.credentials
        val project = parentComponent.getProject()
        object : Task.Backgroundable(project, message("title.submitting.error.report"), false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    doSubmit(project, credentials, event, message, stacktrace, additionalInfo, consumer)
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
        event: IdeaLoggingEvent,
        message: String?,
        stacktrace: String,
        additionalInfo: String?,
        consumer: Consumer<in SubmittedReportInfo>
    ) {
        val issueID = event.getIssueId(stacktrace)
        val (status, issue) = findIssue(issueID)
            ?.let { issue -> SubmittedReportInfo.SubmissionStatus.DUPLICATE to issue }
            ?: postNewIssue(event, credentials, issueID, message, additionalInfo, stacktrace)
                .let { issue -> SubmittedReportInfo.SubmissionStatus.NEW_ISSUE to issue }

        val reportInfo = SubmittedReportInfo(issue.htmlUrl, "Issue#${issue.number}", status)
        invokeLater {
            consumer.consume(reportInfo)
            showIssueNotification(project, reportInfo)
        }
    }

    private fun showIssueNotification(project: Project?, reportInfo: SubmittedReportInfo) {
        val text = StringBuilder().apply {
            append(message("error.report.submitted.as.link", reportInfo.linkText))
            if (reportInfo.status == SubmittedReportInfo.SubmissionStatus.DUPLICATE) {
                append(message("error.report.duplicate"))
            }
            append(". ")
            append(message("error.report.gratitude"))
        }

        val title = message("error.report.submitted")
        val content = XmlStringUtil.wrapInHtml(text)
        val action = BrowseUrlAction(message("error.report.action.text.view", reportInfo.linkText), reportInfo.url)
        ErrorReportNotifications.showNotification(project, title, content, action)
    }

    private fun onError(
        project: Project?,
        e: Exception,
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        callback: Consumer<in SubmittedReportInfo>
    ) {
        LOG.w("reporting failed:", e)
        invokeLater(expired = (project ?: Application).disposed) {
            val title = message("error.report.failed.title")
            if (e is HttpRequests.HttpStatusException &&
                e.statusCode == HttpResponseStatus.UNAUTHORIZED.code() &&
                ReportCredentials.instance.isAnonymous
            ) {
                callback.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
                val message = message("error.report.failed.message.anonymity.disabled")
                val result = MessageDialogBuilder
                    .okCancel(title, message)
                    .icon(UIUtil.getInformationIcon())
                    .yesText(message("error.change.reporter.account.login"))
                    .noText(message("close.action.name"))
                    .ask(project)
                if (result) {
                    changeReporterAccount(parentComponent)
                }
            } else {
                val message = message("error.report.failed.message", e.message.toString())
                val result = MessageDialogBuilder.yesNo(title, message).ask(project)
                if (!result || !submit(events, additionalInfo, parentComponent, callback)) {
                    callback.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
                }
            }
        }
    }

    private fun findIssue(issueId: String): Issue? {
        val query = "repo:$TARGET_REPOSITORY is:issue in:body $issueId"
        val searchResult = GitHubIssuesApis.search(query, page = 1, perPage = 1)
        val issue = searchResult.items.firstOrNull()
        if (issue != null) {
            LOG.d("Issue is actually a duplicate of existing one: $issue")
        }

        return issue
    }

    private fun postNewIssue(
        event: IdeaLoggingEvent,
        credentials: Credentials,
        issueId: String,
        message: String?,
        comment: String?,
        stacktrace: String
    ): Issue {
        val titleMessage = message
            ?.takeIf { it.isNotEmpty() && it != EXCEPTION_CLASS_CHANGED_MESSAGE }
            ?.let { ": ${it.singleLine().compressWhitespace().ellipsis(100)}" }
            ?: ""
        val title = "[Auto Generated]Plugin error occurred$titleMessage"
        val body = StringBuilder()
            .appendLine(":warning:_`[Auto Generated Report]-=$issueId=-`_")
            .appendLine("<!-- Auto Generated Report. DO NOT MODIFY!!! -->\n")
            .appendDescription(event, message, comment, stacktrace)
            .appendEnvironments()
            .appendStacktrace(stacktrace)
            .appendAttachments(event)
            .toString()

        return GitHubIssuesApis.create(TARGET_REPOSITORY, title, body, credentials.getPasswordAsString()!!)
    }

    private fun StringBuilder.appendDescription(
        event: IdeaLoggingEvent, message: String?,
        comment: String?,
        stacktrace: String
    ) = apply {
        appendLine("## Description")
        appendLine()

        val summary = event.getDataRedactedSummary(stacktrace)
        if (!summary.isNullOrEmpty()) {
            appendLine(summary)
            appendLine()
        }
        if (!message.isNullOrEmpty()) {
            appendLine(message)
            appendLine()
        }
        if (!comment.isNullOrBlank()) {
            appendLine("### Additional Information")
            appendLine(comment)
            appendLine()
        }
    }

    private fun StringBuilder.appendEnvironments() = apply {
        appendLine("## Environments")
        append("> **Plugin version: ", TranslationPlugin.version, "**", "\n\n")

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

    private fun StringBuilder.appendStacktrace(stacktrace: String) = apply {
        appendLine()
        appendLine("## Stack Trace")
        appendLine("```")
        appendLine(stacktrace)
        appendLine("```")
    }

    private fun StringBuilder.appendAttachments(event: IdeaLoggingEvent) = apply {
        val attachments = (event.data as? AbstractMessage)?.includedAttachments
            ?.takeIf { it.isNotEmpty() }
            ?: return@apply

        appendLine()
        appendLine("## Attachments")
        for (attachment in attachments) {
            appendAttachment(attachment)
        }
    }

    private fun StringBuilder.appendAttachment(attachment: Attachment) = apply {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(attachment.name) as? LanguageFileType
        val language = fileType?.language?.displayName ?: ""

        appendLine("<details>")
        appendLine("<summary>${attachment.path}</summary>")
        appendLine()
        appendLine("```$language")
        appendLine(attachment.displayText)
        appendLine("```")
        appendLine()
        appendLine("</details>")
    }

    private fun String.removeCR(): String = replace("\r", "")

    private fun IdeaLoggingEvent.getUsefulMessage(): String? {
        return (message?.removeCR() ?: (data as? AbstractMessage)?.throwable?.message)?.trim()
    }

    private val IdeaLoggingEvent.stacktrace: String get() = throwableText?.trim()?.removeCR() ?: ""
    private val IdeaReportingEvent.originalStacktrace: String get() = originalThrowableText.trim().removeCR()

    private fun IdeaLoggingEvent.getIssueId(stacktrace: String): String {
        if (this is IdeaReportingEvent && stacktrace != originalStacktrace) {
            return stacktrace.md5()
        }

        return (data as? AbstractMessage)?.throwable?.generateId() ?: stacktrace.md5()
    }

    private fun IdeaLoggingEvent.getDataRedactedSummary(stacktrace: String): String? {
        if (this !is IdeaReportingEvent) {
            return null
        }

        val originalMessage = originalMessage?.trim()?.removeCR() ?: ""
        val message = message?.trim()?.removeCR() ?: ""
        val originalStacktrace = originalStacktrace
        val messagesDiffer = originalMessage != message
        val tracesDiffer = stacktrace != originalStacktrace
        if (messagesDiffer || tracesDiffer) {
            var summary = ""
            if (messagesDiffer) summary += "*** message was redacted (${diff(originalMessage, message)})\n"
            if (tracesDiffer) summary += "*** stacktrace was redacted (${diff(originalStacktrace, stacktrace)})\n"
            return summary.trim()
        }

        return null
    }

    private fun diff(original: String, redacted: String): String {
        return "original:${original.wc()} submitted:${redacted.wc()}"
    }

    private fun String.wc(): String = if (isEmpty()) {
        "-"
    } else {
        "${StringUtil.splitByLines(this).size}/${split("[^\\w']+".toRegex()).size}/$length"
    }
}