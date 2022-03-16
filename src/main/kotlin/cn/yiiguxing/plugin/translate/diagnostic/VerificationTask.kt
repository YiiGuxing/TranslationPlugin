package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Http
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import javax.swing.JComponent

internal class VerificationTask(project: Project?, component: JComponent) :
    Task.WithResult<GitHubVerification, Exception>(
        project, component, message("error.account.device.code.task.title"), true
    ) {

    override fun compute(indicator: ProgressIndicator): GitHubVerification {
        indicator.checkCanceled()
        return Http.post<GitHubVerification>(
            DEVICE_CODE_URL,
            "client_id" to GITHUB_CLIENT_ID, "scope" to GITHUB_OAUTH_SCOPES
        ).also { indicator.checkCanceled() }
    }

    fun queueAndGet(): GitHubVerification? {
        return try {
            ProgressManager.getInstance().run(this)
        } catch (e: ProcessCanceledException) {
            null
        }
    }


    companion object {
        private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
    }
}