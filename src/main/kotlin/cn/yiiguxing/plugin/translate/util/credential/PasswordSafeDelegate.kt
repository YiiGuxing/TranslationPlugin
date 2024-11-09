package cn.yiiguxing.plugin.translate.util.credential

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * Delegate for storing and retrieving password in [PasswordSafe].
 */
class PasswordSafeDelegate(serviceName: String) : ReadWriteProperty<Any?, String?> {

    private val store: PasswordSafe by lazy { PasswordSafe.instance }
    // should be the only credentials per service name, no need to specify a username. see: BaseKeePassCredentialStore
    private val attributes: CredentialAttributes = CredentialAttributes(serviceName)

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = store.getPassword(attributes)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        store.setPassword(attributes, value)
    }
}