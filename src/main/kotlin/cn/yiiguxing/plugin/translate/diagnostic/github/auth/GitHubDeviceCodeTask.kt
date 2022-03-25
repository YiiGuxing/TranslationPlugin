package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import cn.yiiguxing.plugin.translate.compat.progress.TaskWithResultCompat
import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import javax.swing.JComponent

internal class GitHubDeviceCodeTask(
    project: Project?,
    parentComponent: JComponent?,
    private val clientId: String,
    private vararg val scopes: String
) :
    TaskWithResultCompat<GitHubDeviceCode, Exception>(
        project, parentComponent, message("github.retrieving.device.code.task.title"), true
    ) {

    override fun compute(indicator: ProgressIndicator): GitHubDeviceCode {
        indicator.checkCanceled()
        return GitHubDeviceAuthApis.getDeviceCode(clientId, *scopes)
            .also { indicator.checkCanceled() }
    }
}