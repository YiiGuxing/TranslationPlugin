package cn.yiiguxing.plugin.translate.ui.util

import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import org.jetbrains.concurrency.runAsync
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class CredentialEditor(manager: () -> StringCredentialManager) : Disposable {
    private var originalApiKey: String? = null
    private var apiKey: String? = null
    private var isLoaded: Boolean = false

    private val manager: StringCredentialManager by lazy { manager() }

    private var editor: JBPasswordField? = null
    private val documentListener = object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            editor?.let {
                apiKey = String(it.password)
                it.verify()
            }
        }
    }

    val isCredentialSet: Boolean
        get() = manager.isCredentialSet

    @Suppress("unused")
    constructor(manager: StringCredentialManager) : this({ manager })

    fun startEditing(field: JBPasswordField) {
        editor = field
        field.document.addDocumentListener(documentListener)

        if (isLoaded) {
            update(apiKey)
        } else {
            field.isEnabled = false
            load()
        }
    }

    fun stopEditing() {
        editor?.document?.removeDocumentListener(documentListener)
        editor = null
    }

    fun applyEditing() {
        if (isLoaded && apiKey != originalApiKey) {
            manager.credential = apiKey
        }
    }

    private fun load() {
        val editor = editor ?: return
        asyncLatch { latch ->
            runAsync {
                latch.await()
                manager.credential
            }
                .expireWith(this)
                .successOnUiThread(ModalityState.stateForComponent(editor)) {
                    isLoaded = true
                    apiKey = it
                    originalApiKey = it
                    update(it)
                }
        }
    }

    private fun update(apiKey: String?) {
        editor?.let {
            it.text = apiKey.orEmpty()
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