package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.ProgressManagerImpl
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class GoogleSettingsDialog : DialogWrapper(true) {

    private val settings = service<GoogleSettings>()

    private val customServerCheckBox = JBCheckBox(message("google.settings.dialog.label.server"))
    private val serverUrlField = JBTextField().apply {
        emptyText.text = message("google.settings.dialog.tip.enter.server.url")
    }
    private val testButton = JButton(message("action.test.text"))

    private val alarm: Alarm = Alarm(disposable)

    private val updateAction = Runnable { update() }

    private var serverUrl: String?
        get() = serverUrlField.text?.takeIf { it.isNotBlank() }
        set(value) {
            serverUrlField.text = value?.trim() ?: ""
        }

    init {
        title = message("google.settings.dialog.title")
        setResizable(false)

        init()
        initListeners()

        serverUrl = settings.serverUrl
        customServerCheckBox.isSelected = settings.customServer
        update()
    }

    private fun initListeners() {
        customServerCheckBox.addChangeListener { update() }
        serverUrlField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                alarm.cancelAllRequests()
                alarm.addRequest(updateAction, 300)
            }
        })
        testButton.addActionListener { testServer() }
    }

    private fun update() {
        val customServer = customServerCheckBox.isSelected
        serverUrlField.isEnabled = customServer

        val serverUrlVerification = verifyServerUrl()
        testButton.isEnabled = customServer && serverUrlVerification && serverUrl != null
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(VerticalLayout(JBUIScale.scale(4))).apply {
            add(createLogoPane())
            add(createConfigPanel())
        }
    }

    private fun createLogoPane(): JComponent {
        return JLabel(TranslationIcons.load("image/google_translate_logo.svg")).apply {
            border = JBUI.Borders.empty(10, 0, 18, 0)
        }
    }

    private fun createConfigPanel(): JComponent {
        return JPanel(UI.migLayout("${JBUIScale.scale(4)}")).apply {
            add(customServerCheckBox)
            add(serverUrlField, UI.fillX().minWidth("${JBUIScale.scale(400)}px"))
            add(testButton)
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
        val url = serverUrl
        if (url == null || URL_REGEX.matches(url)) {
            setErrorText(null)
            return true
        }

        setErrorText(message("google.settings.dialog.error.invalid.server.url"), serverUrlField)
        return false
    }

    private fun testServer() {
        val url = serverUrl ?: return
        val title = message("google.settings.test.result.title")
        try {
            TestTask(url).run(rootPane)

            val message = message("google.settings.test.result.success.message")
            Messages.showInfoMessage(rootPane, message, title)
        } catch (e: ProcessCanceledException) {
            // ignore
        } catch (e: Throwable) {
            thisLogger().w("Failed to test server: $url", e)
            val errorMessage = message("google.settings.test.result.error.message", e.message ?: "")
            Messages.showErrorDialog(rootPane, errorMessage, title)
        }
    }

    private class TestTask(private val serverUrl: String) :
        Task.WithResult<Unit, Exception>(null, message("google.settings.test.task.title"), true) {
        override fun compute(indicator: ProgressIndicator) {
            indicator.isIndeterminate = true
            indicator.checkCanceled()
            TKK.fetchTKK(serverUrl)
            indicator.checkCanceled()
        }

        fun run(parentComponent: JComponent) {
            val progressManager = ProgressManager.getInstance() as ProgressManagerImpl
            progressManager.runProcessWithProgressSynchronously(this, parentComponent)
            getResult()
        }
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()
    }
}