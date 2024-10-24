package cn.yiiguxing.plugin.translate.util.credential

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * Delegate for storing and retrieving password in [PasswordSafe].
 */
class PasswordSafeDelegate(serviceName: String, userName: String? = null) : ReadWriteProperty<Any?, String?> {

    private val store: PasswordSafe by lazy { PasswordSafe.instance }
    private val attributes: CredentialAttributes = CredentialAttributes(serviceName, userName)

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = store.getPassword(attributes)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        store.setPassword(attributes, value)
    }
}