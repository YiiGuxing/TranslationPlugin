package cn.yiiguxing.plugin.translate.diagnostic.github

import cn.yiiguxing.plugin.translate.diagnostic.github.auth.*
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import javax.swing.JComponent


@Service
internal class TranslationGitHubAppService private constructor() {

    companion object {
        private const val CLIENT_ID = "e8a353548fe014bb27de"

        private val SCOPES: Array<out String> = arrayOf("public_repo")

        val instance: TranslationGitHubAppService = service()
    }


    @RequiresEdt
    fun auth(project: Project?, parentComponent: JComponent?): GitHubCredentials? {
        Application.assertIsDispatchThread()

        val deviceCode = getDeviceCode(project, parentComponent) ?: return null
        val ok = GitHubDeviceLoginDialog(project, parentComponent, deviceCode).showAndGet()
        return if (ok) authorize(project, parentComponent, deviceCode) else null
    }

    private fun getDeviceCode(project: Project?, parentComponent: JComponent?): GitHubDeviceCode? {
        return try {
            GitHubDeviceCodeTask(project, parentComponent, CLIENT_ID, *SCOPES).queueAndGet()
        } catch (e: Exception) {
            val message = message("github.retrieving.device.code.failed.message", e.message.toString())
            throw TranslationGitHubAppException(message, e)
        }
    }

    private fun authorize(
        project: Project?,
        parentComponent: JComponent?,
        deviceCode: GitHubDeviceCode
    ): GitHubCredentials? {
        return try {
            GitHubDeviceAuthTask(project, parentComponent, CLIENT_ID, deviceCode).queueAndGet()
        } catch (e: Exception) {
            val errorMessage = (e as? GitHubDeviceAuthException)?.errorDescription ?: e.message ?: ""
            val message = message("github.authentication.failed.message", errorMessage)
            throw TranslationGitHubAppException(message, e)
        }
    }

}