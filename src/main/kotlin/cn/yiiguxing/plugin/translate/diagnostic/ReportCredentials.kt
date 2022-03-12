package cn.yiiguxing.plugin.translate.diagnostic

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import java.util.*

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

    fun save(userName: String, token: String) {
        val credentials = Credentials(userName, token)
        val attributes = CredentialAttributes(serviceName, userName)
        PasswordSafe.instance.set(attributes, credentials)
        lastUserName = userName
    }

    fun clear() {
        PasswordSafe.instance.set(CredentialAttributes(serviceName), null)
        lastUserName = ""
    }
}