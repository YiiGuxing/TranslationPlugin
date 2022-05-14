package cn.yiiguxing.plugin.translate.diagnostic

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.util.*

@Service
internal class ReportCredentials private constructor() {

    companion object {

        // https://github.com/settings/tokens/new?description=Bug+Reporting+Access+Token+for+Translation+Plugin+-+v%3Cversion%3E&scopes=public_repo
        // Base64 encoded token to prevent detection by GitHub Advanced Security, which could lead to token revocation.
        // Version: 2
        @Suppress("SpellCheckingInspection")
        private const val ANONYMOUS_ACCOUNT_TOKEN = "dG9rZW4gZ2hwX2Z4cmVONjQ0UVJQOUxhQUZnS3dRQ0ZiTGxvOXdPVTAxMEtmRA=="

        private val SERVICE_NAME = generateServiceName("TranslationPlugin", "Report Account")

        val instance: ReportCredentials get() = service()
    }


    private val anonymousCredentials: Credentials
        get() = Credentials("", Base64.getDecoder().decode(ANONYMOUS_ACCOUNT_TOKEN))

    @Volatile
    private var lastUserName: String? = null

    val userName: String
        get() = lastUserName ?: (credentials.userName ?: "").also { lastUserName = it }

    val isAnonymous: Boolean
        get() = userName.isEmpty()

    val credentials: Credentials
        get() = PasswordSafe.instance.get(CredentialAttributes(SERVICE_NAME)) ?: anonymousCredentials

    fun clear() {
        PasswordSafe.instance.set(CredentialAttributes(SERVICE_NAME), null)
        lastUserName = ""
    }

    fun save(userName: String, token: String) {
        check(userName.isNotBlank()) { "User name must not be blank" }
        val credentials = Credentials(userName, token)
        val attributes = CredentialAttributes(SERVICE_NAME, userName)
        PasswordSafe.instance.set(attributes, credentials)
        lastUserName = userName
    }

}