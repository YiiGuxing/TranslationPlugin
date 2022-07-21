package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.util.PasswordSafeDelegate
import cn.yiiguxing.plugin.translate.util.Plugin
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class DeeplCredentials private constructor() {

    companion object {
        private val SERVICE_NAME = generateServiceName("DeepL Credentials", "${Plugin.PLUGIN_ID}.DEEPL_AUTH_KEY")

        val instance: DeeplCredentials get() = service()
    }

    private var _uthKey: String? by PasswordSafeDelegate(SERVICE_NAME)

    private var _isAuthKeySet: Boolean? = null

    val isAuthKeySet: Boolean
        @Synchronized get() {
            if (_isAuthKeySet == null) {
                authKey
            }
            return _isAuthKeySet!!
        }

    var authKey: String?
        @Synchronized get() = _uthKey?.takeIf { it.isNotEmpty() }.also {
            _isAuthKeySet = it != null
        }
        @Synchronized set(value) {
            val isNullOrEmptyValue = value.isNullOrEmpty()
            _uthKey = value.takeUnless { isNullOrEmptyValue }
            _isAuthKeySet = !isNullOrEmptyValue
        }

}