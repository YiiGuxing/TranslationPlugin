package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.message
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import java.util.*
import javax.swing.JComponent

internal object ReportCredentials {

    // Base64 encoded token to prevent detection by GitHub Advanced Security, which could lead to token revocation.
    @Suppress("SpellCheckingInspection")
    private const val ANONYMOUS_ACCOUNT_TOKEN = "dG9rZW4gZ2hwX2ZrQ3JGblJ4ZXJ4MlNXNEVJTHJMRUo3N1o3NHJ0dTRXNGptTw=="

    private val serviceName = generateServiceName("TranslationPlugin", "Report Account")

    private val anonymousCredentials: Credentials
        get() = Credentials("", Base64.getDecoder().decode(ANONYMOUS_ACCOUNT_TOKEN))

    private var lastUserName: String? = null

    val userName: String
        get() = lastUserName ?: (credentials.userName ?: "").also { lastUserName = it }

    val isAnonymous: Boolean
        get() = userName.isEmpty()

    val credentials: Credentials
        get() = PasswordSafe.instance.get(CredentialAttributes(serviceName)) ?: anonymousCredentials

    fun clear() {
        PasswordSafe.instance.set(CredentialAttributes(serviceName), null)
        lastUserName = ""
    }

    private fun save(userName: String, token: String): Credentials {
        check(userName.isNotBlank()) { "User name must not be blank" }
        val credentials = Credentials(userName, token)
        val attributes = CredentialAttributes(serviceName, userName)
        PasswordSafe.instance.set(attributes, credentials)
        lastUserName = userName

        return credentials
    }

    fun requestNewCredentials(project: Project?, parentComponent: JComponent): Credentials? {
        val verification = getVerification(project, parentComponent) ?: return null
        println(verification)

        // TODO 认证

        return save("", "")
    }

    private fun getVerification(project: Project?, parentComponent: JComponent): GitHubVerification? {
        return try {
            VerificationTask(project, parentComponent).queueAndGet()
        } catch (e: Exception) {
            val title = message("error.account.change.failed.title")
            val message = message("error.account.verification.failed.message", e.message.toString())
            if (MessageDialogBuilder.yesNo(title, message).ask(project)) {
                getVerification(project, parentComponent)
            } else {
                null
            }
        }
    }
}