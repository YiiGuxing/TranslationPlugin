package cn.yiiguxing.plugin.translate.util

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * PasswordSafeDelegate
 */
class PasswordSafeDelegate private constructor(private val attributes: CredentialAttributes) :
    ReadWriteProperty<Any?, String?> {

    constructor(serviceName: String, userName: String? = null) :
            this(CredentialAttributesFactory.create(serviceName, userName))

    private val store = PasswordSafe.getInstance()

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = store.getPassword(attributes)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        store.setPassword(attributes, value)
    }
}