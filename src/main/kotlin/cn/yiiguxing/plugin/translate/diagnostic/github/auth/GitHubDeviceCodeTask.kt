package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.project.Project
import javax.swing.JComponent

internal class GitHubDeviceCodeTask(
    project: Project?,
    private val clientId: String,
    private vararg val scopes: String
) :
    Task.WithResult<GitHubDeviceCode, Exception>(
        project, message("github.retrieving.device.code.task.title"), true
    ) {

    override fun compute(indicator: ProgressIndicator): GitHubDeviceCode {
        indicator.checkCanceled()
        return GitHubDeviceAuthApis.getDeviceCode(clientId, *scopes)
            .also { indicator.checkCanceled() }
    }

    fun queueAndGet(parentComponent: JComponent?): GitHubDeviceCode? {
        return try {
            val progressManager = ProgressManager.getInstance() as ProgressManagerImpl
            progressManager.runProcessWithProgressSynchronously(this, parentComponent)
            result
        } catch (e: ProcessCanceledException) {
            null
        }
    }
}