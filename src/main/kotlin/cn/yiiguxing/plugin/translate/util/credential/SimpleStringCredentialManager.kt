package cn.yiiguxing.plugin.translate.util.credential

import cn.yiiguxing.plugin.translate.util.PasswordSafeDelegate
import com.intellij.credentialStore.generateServiceName

/**
 * Simple implementation of [StringCredentialManager].
 *
 * @param serviceName The service name. Please consider using [generateServiceName] to generate.
 * @see generateServiceName
 */
class SimpleStringCredentialManager(serviceName: String) : StringCredentialManager {

    private var _credential: String? by PasswordSafeDelegate(serviceName)

    private var _isCredentialSet: Boolean? = null

    override var credential: String?
        @Synchronized get() = _credential?.takeIf { it.isNotEmpty() }.also {
            _isCredentialSet = it != null
        }
        @Synchronized set(value) {
            val isNullOrEmptyValue = value.isNullOrEmpty()
            _credential = value.takeUnless { isNullOrEmptyValue }
            _isCredentialSet = !isNullOrEmptyValue
        }

    override val isCredentialSet: Boolean
        @Synchronized get() {
            if (_isCredentialSet == null) {
                credential
            }
            return _isCredentialSet!!
        }

}