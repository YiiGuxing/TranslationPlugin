package cn.yiiguxing.plugin.translate.trans.deepl

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.errorOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.Alarm
import com.intellij.util.io.HttpRequests
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import java.awt.Color
import java.io.IOException
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

class DeeplSettingsDialog : DialogWrapper(false) {

    companion object {
        private val ERROR_FOREGROUND_COLOR = UIUtil.getErrorForeground()
        private val WARNING_FOREGROUND_COLOR = JBColor(Color(0xFB8C00), Color(0xF0A732))
    }

    private val authKeyField: JBPasswordField = JBPasswordField()
    private val charactersCount: JBLabel = JBLabel("-")
    private val characterLimit: JBLabel = JBLabel("-")
    private val usageInfoPanel: JPanel = createUsageInfoPanel()

    private var currentService: DeeplService? = null
    private val alarm: Alarm = Alarm(disposable)

    private var authKey: String?
        get() = authKeyField.password
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it) }
        set(value) {
            authKeyField.text = if (value.isNullOrEmpty()) null else value
        }


    init {
        title = message("deepl.settings.dialog.title")
        isResizable = false
        init()

        updateUsageInfo(null)
        authKey = DeeplCredential.authKey
        initListeners()
    }

    private fun initListeners() {
        val getUsageInfoAction = ::doGetUsageInfo
        authKeyField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                alarm.cancelAllRequests()
                alarm.addRequest(getUsageInfoAction, 500)
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/deepl_translate_logo.svg")
        return LogoHeaderPanel(logo).apply {
            add(createAuthPanel())
            add(usageInfoPanel)
        }
    }

    private fun createAuthPanel(): JPanel {
        val authKeyFieldWidth = 300
        return JPanel(UI.migLayout(UI.migSize(8))).apply {
            add(JLabel(message("deepl.settings.dialog.label.auth.key")))
            add(authKeyField, UI.fillX().width(UI.migSize(authKeyFieldWidth)).wrap())
            add(
                UI.createHint(message("deepl.settings.dialog.hint"), authKeyFieldWidth, authKeyField),
                UI.cc().cell(1, 1).wrap()
            )
        }
    }

    private fun createUsageInfoPanel(): JPanel {
        return JPanel(UI.migLayout(UI.migSize(8), UI.migSize(4))).apply {
            border = IdeBorderFactory.createTitledBorder(message("deepl.settings.dialog.label.usage.information"))

            add(JBLabel(message("deepl.settings.dialog.label.translated")))
            add(charactersCount)
            add(JBLabel(message("deepl.settings.dialog.label.characters")), UI.fillX().wrap())

            add(JBLabel(message("deepl.settings.dialog.label.quota")))
            add(characterLimit)
            add(JBLabel(message("deepl.settings.dialog.label.characters")), UI.fillX().wrap())
        }
    }

    private fun updateUsageInfo(usage: DeeplService.Usage?, isFreeAccount: Boolean = false) {
        charactersCount.text = usage?.characterCount?.toString() ?: "-"
        characterLimit.text = usage?.characterLimit?.toString() ?: "-"

        val title = message("deepl.settings.dialog.label.usage.information")
        if (usage != null) {
            val accountType = if (isFreeAccount) "FREE" else "PRO"
            usageInfoPanel.border = IdeBorderFactory.createTitledBorder("$title - $accountType")

            charactersCount.foreground = when {
                usage.limitReached -> ERROR_FOREGROUND_COLOR
                (usage.characterCount.toFloat() / usage.characterLimit.toFloat()) >= 0.8f -> WARNING_FOREGROUND_COLOR
                else -> JBUI.CurrentTheme.Label.foreground()
            }
            characterLimit.foreground = JBUI.CurrentTheme.Label.foreground()
        } else {
            usageInfoPanel.border = IdeBorderFactory.createTitledBorder(title)

            val foreground = JBUI.CurrentTheme.Label.disabledForeground()
            charactersCount.foreground = foreground
            characterLimit.foreground = foreground
        }
    }

    private fun doGetUsageInfo() {
        val authKey = authKey
        if (authKey.isNullOrEmpty()) {
            currentService = null
            updateUsageInfo(null)
            setErrorText(message("deepl.settings.dialog.message.enter.auth.key"), authKeyField)
            return
        }
        if (authKey == currentService?.authKey) {
            return
        }

        val service = DeeplService(authKey)
        currentService = service
        postUsageInfo(service, null)

        val dialogRef = DisposableRef.create(disposable, this)
        runAsync { service.getUsage() }
            .successOnUiThread(dialogRef) { dialog, usage ->
                dialog.postUsageInfo(service, usage)
                dialogRef.disposeSelf()
            }
            .errorOnUiThread(dialogRef) { dialog, error ->
                thisLogger().w("Failed to get usage info.", error)
                dialog.postUsageInfo(service, null, error)
                dialogRef.disposeSelf()
            }
    }

    private fun postUsageInfo(service: DeeplService, usage: DeeplService.Usage?, throwable: Throwable? = null) {
        if (service.authKey != currentService?.authKey) {
            return
        }

        updateUsageInfo(usage, service.isFreeAccount)

        if (throwable == null) {
            setErrorText(null)
            return
        }

        if (throwable is HttpRequests.HttpStatusException && throwable.statusCode == 403) {
            setErrorText(message("error.invalid.authentication.key"), authKeyField)
        } else {
            val message = (throwable as? IOException)
                ?.getCommonMessage()
                ?: throwable.message
                ?: message("error.unknown")
            setErrorText(message)
        }
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater { doGetUsageInfo() }
        super.show()
    }

    override fun getHelpId(): String = HelpTopic.DEEPL.id

    override fun isOK(): Boolean = DeeplCredential.isAuthKeySet

    override fun doOKAction() {
        DeeplCredential.authKey = authKey
        super.doOKAction()
    }
}
