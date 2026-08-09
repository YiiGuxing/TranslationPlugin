package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.Alarm
import icons.TranslationIcons
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class GoogleSettingsDialog : DialogWrapper(true) {

    private val settings = service<GoogleSettings>()

    private val customServerCheckBox = JBCheckBox(message("google.settings.dialog.label.server"))
    private val serverUrlField = JBTextField().apply {
        emptyText.text = DEFAULT_GOOGLE_API_SERVER_URL
    }
    private val testButton = JButton(message("action.test.text"))

    private val alarm: Alarm = Alarm(disposable)

    private var serverUrl: String?
        get() = serverUrlField.text?.takeIf { it.isNotBlank() }
        set(value) {
            serverUrlField.text = value?.trim() ?: ""
        }

    init {
        title = message("google.settings.dialog.title")
        isResizable = false

        init()
        initListeners()

        serverUrl = settings.serverUrl
        customServerCheckBox.isSelected = settings.customServer
        update()
    }

    private fun initListeners() {
        customServerCheckBox.addItemListener {
            update()
            if (customServerCheckBox.isSelected) {
                IdeFocusManager.getInstance(null).requestFocus(serverUrlField, true)
            }
        }

        val updateAction = ::update
        serverUrlField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                alarm.cancelAllRequests()
                alarm.addRequest(updateAction, 300)
            }
        })
    }

    private fun update() {
        val customServer = customServerCheckBox.isSelected
        serverUrlField.isEnabled = customServer

        val serverUrlVerification = verifyServerUrl()
        testButton.isEnabled = customServer && serverUrlVerification && serverUrl != null
        okAction.isEnabled = serverUrlVerification
    }

    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/google_translate_logo.svg")
        val content = createConfigurationPanel()
        return LogoHeaderPanel(logo, content)
    }

    private fun createConfigurationPanel(): JComponent {
        return JPanel(UI.migLayout(UI.migSize(4))).apply {
            add(customServerCheckBox)
            add(serverUrlField, UI.cc().width(UI.migSize(400)))
        }
    }

    override fun isOK(): Boolean = true

    override fun doOKAction() {
        if (!verifyServerUrl()) {
            return
        }

        settings.serverUrl = serverUrl
        settings.customServer = serverUrl != null && customServerCheckBox.isSelected

        super.doOKAction()
    }

    private fun verifyServerUrl(): Boolean {
        if (!customServerCheckBox.isSelected || serverUrl.let { it == null || URL_REGEX.matches(it) }) {
            setErrorText(null)
            return true
        }

        setErrorText(message("google.settings.dialog.error.invalid.server.url"), serverUrlField)
        return false
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()
    }
}