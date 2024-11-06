@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.ui.util

import cn.yiiguxing.plugin.translate.util.asReadOnly
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import cn.yiiguxing.plugin.translate.util.observe
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import org.jetbrains.concurrency.runAsync
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

/**
 * The editor for editing credentials.
 */
class CredentialEditor private constructor(
    managerLazy: Lazy<StringCredentialManager>,
    parent: Disposable?
) : Disposable {
    private val manager: StringCredentialManager by managerLazy

    private var originalCredential: String? = null
    private val _credentialBinding = observe<String?>(null)
    val credentialBinding = _credentialBinding.asReadOnly()
    var credential: String? by _credentialBinding
        private set

    private val _loadedBinding = observe(false)
    val loadedBinding = _loadedBinding.asReadOnly()
    var isLoaded: Boolean by _loadedBinding
        private set

    private var editor: JBPasswordField? = null
    private val documentListener = object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            editor?.let { editor ->
                credential = editor.password?.takeIf { it.isNotEmpty() }?.let { String(it) }
                editor.verify()
            }
        }
    }

    val isCredentialSet: Boolean
        get() = manager.isCredentialSet

    constructor(manager: StringCredentialManager, parent: Disposable? = null) : this(lazyOf(manager), parent)

    constructor(parent: Disposable? = null, manager: () -> StringCredentialManager) : this(lazy { manager() }, parent)

    init {
        parent?.let { Disposer.register(it, this) }
    }

    /**
     * Start editing the credential.
     */
    fun startEditing(field: JBPasswordField) {
        editor = field
        field.document.addDocumentListener(documentListener)

        if (isLoaded) {
            update(credential)
        } else {
            load()
        }
    }

    /**
     * Stop editing the credential.
     */
    fun stopEditing() {
        editor?.document?.removeDocumentListener(documentListener)
        editor = null
    }

    /**
     * Apply the editing. Returns `true` if the credential has been changed.
     */
    fun applyEditing(): Boolean {
        if (isLoaded && credential != originalCredential) {
            manager.credential = credential
            return true
        }

        return false
    }

    private fun load() {
        val editor = editor ?: return

        editor.text = ""
        editor.isEnabled = false
        asyncLatch { latch ->
            runAsync {
                latch.await()
                manager.credential
            }
                .expireWith(this)
                .successOnUiThread(ModalityState.stateForComponent(editor)) {
                    isLoaded = true
                    credential = it
                    originalCredential = it
                    update(it)
                }
        }
    }

    private fun update(credential: String?) {
        editor?.let {
            it.text = credential.orEmpty()
            it.isEnabled = true
            it.verify()
        }
    }

    override fun dispose() {
        stopEditing()
    }

    companion object {
        private fun JComponent.verify() {
            ComponentValidator.getInstance(this).ifPresent { it.revalidate() }
        }
    }
}