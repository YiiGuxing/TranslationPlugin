package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Http
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import javax.swing.JComponent

internal class AuthenticationTask(
    project: Project?,
    component: JComponent,
    private val verification: GitHubVerification
) :
    Task.WithResult<GitHubToken, Exception>(
        project, component, message("error.account.authentication.title"), true
    ) {

    override fun compute(indicator: ProgressIndicator): GitHubToken {
        indicator.text = message("error.account.authentication.message")
        indicator.checkCanceled()
        return Http.post<GitHubToken>(
            OAUTH_URL,
            "client_id" to GITHUB_CLIENT_ID,
            "device_code" to verification.deviceCode,
            "grant_type" to GRANT_TYPE
        ).also { indicator.checkCanceled() }
    }

    fun queueAndGet(): GitHubToken? {
        return try {
            ProgressManager.getInstance().run(this)
        } catch (e: ProcessCanceledException) {
            null
        }
    }


    companion object {
        private const val OAUTH_URL = "https://github.com/login/oauth/access_token"
        private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
    }
}