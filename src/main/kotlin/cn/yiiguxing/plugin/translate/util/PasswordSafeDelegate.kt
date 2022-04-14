package cn.yiiguxing.plugin.translate.util

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * PasswordSafeDelegate
 */
class PasswordSafeDelegate(serviceName: String) : ReadWriteProperty<Any?, String?> {

    private val store = PasswordSafe.instance
    private val attributes: CredentialAttributes = CredentialAttributes(serviceName)

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = store.getPassword(attributes)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        store.setPassword(attributes, value)
    }
}