package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplCredential
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplService
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.Alarm
import com.intellij.util.io.HttpRequests
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import java.awt.Color
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent

class DeeplConfigurationDialog : DialogWrapper(false) {

    private val authKeyField: JBPasswordField = JBPasswordField()

    private val charactersCount: JBLabel = JBLabel("-")
    private val charactersCountUnit: JBLabel = JBLabel("")
    private val charactersLimit: JBLabel = JBLabel("-")
    private val charactersLimitUnit: JBLabel = JBLabel("")
    private val usageInfoPanel: JPanel = createUsageInfoPanel()

    private var currentService: DeeplService? = null
    private val alarm: Alarm = Alarm(disposable)

    private val logger = Logger.getInstance(DeeplConfigurationDialog::class.java)

    private var authKey: String?
        get() = authKeyField.password
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it) }
        set(value) {
            authKeyField.text = if (value.isNullOrEmpty()) null else value
        }


    init {
        title = message("deepl.config.dialog.title")
        setResizable(false)
        init()

        updateUsageInfo(null)
        authKey = DeeplCredential.authKey
        initListeners()
    }

    private fun initListeners() {
        val getUsageInfoAction = ::doGetUsageInfo
        authKeyField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                alarm.apply {
                    cancelAllRequests()
                    addRequest(getUsageInfoAction, 500)
                }
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(VerticalLayout(JBUIScale.scale(4))).apply {
            add(createLogoPane())
            add(createAuthPanel())
            add(usageInfoPanel)
        }
    }

    private fun createLogoPane(): JComponent {
        return JLabel(TranslationIcons.load("/image/deepl_translate_logo.svg")).apply {
            border = JBUI.Borders.empty(10, 0, 18, 0)
        }
    }

    private fun createAuthPanel(): JPanel {
        return JPanel(UI.migLayout("${JBUIScale.scale(8)}")).apply {
            add(JLabel(message("deepl.config.dialog.label.auth.key")))
            add(authKeyField, UI.fillX().wrap())
            add(createHintPane(), UI.fillX().cell(1, 1).wrap())
        }
    }

    @Suppress("DuplicatedCode")
    private fun createHintPane(): JComponent = JEditorPane().apply {
        isEditable = false
        isFocusable = false
        isOpaque = false
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        font = font.deriveFont((font.size - 1).toFloat())
        editorKit = UIUtil.getHTMLEditorKit()
        border = JBUI.Borders.emptyTop(2)
        text = message("deepl.config.dialog.hint")
        preferredSize = JBUI.size(300, -1)
        minimumSize = JBUI.size(300, 40)
        maximumSize = JBUI.size(300, Int.MAX_VALUE)

        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    private fun createUsageInfoPanel(): JPanel {
        val gapX = JBUIScale.scale(8).toString()
        val gapY = JBUIScale.scale(4).toString()
        return JPanel(UI.migLayout(gapX, gapY)).apply {
            border = IdeBorderFactory.createTitledBorder(message("deepl.config.dialog.label.usage.information"))

            add(JBLabel(message("deepl.config.dialog.label.translated")))
            add(charactersCount)
            add(charactersCountUnit, UI.fillX().wrap())

            add(JBLabel(message("deepl.config.dialog.label.quota")))
            add(charactersLimit)
            add(charactersLimitUnit, UI.fillX().wrap())
        }
    }

    private fun updateUsageInfo(usage: DeeplService.Usage?, isFreeAccount: Boolean = false) {
        charactersCount.text = usage?.characterCount?.toString() ?: "-"
        charactersLimit.text = usage?.characterLimit?.toString() ?: "-"
        charactersCountUnit.text = getCharUnit(usage?.characterCount)
        charactersLimitUnit.text = getCharUnit(usage?.characterLimit)

        val title = message("deepl.config.dialog.label.usage.information")
        if (usage != null) {
            val accountType = if (isFreeAccount) "FREE" else "PRO"
            usageInfoPanel.border = IdeBorderFactory.createTitledBorder("$title - $accountType")

            charactersCount.foreground = when {
                usage.limitReached -> ERROR_FOREGROUND_COLOR
                (usage.characterCount.toFloat() / usage.characterLimit.toFloat()) >= 0.8f -> WARNING_FOREGROUND_COLOR
                else -> JBUI.CurrentTheme.Label.foreground()
            }
            charactersLimit.foreground = JBUI.CurrentTheme.Label.foreground()
        } else {
            usageInfoPanel.border = IdeBorderFactory.createTitledBorder(title)

            val foreground = JBUI.CurrentTheme.Label.disabledForeground()
            charactersCount.foreground = foreground
            charactersLimit.foreground = foreground
        }
    }

    private fun getCharUnit(value: Int?): String {
        return if (value != null && value > 1) {
            message("deepl.config.dialog.label.characters")
        } else {
            message("deepl.config.dialog.label.character")
        }
    }

    private fun doGetUsageInfo() {
        val authKey = authKey
        if (authKey.isNullOrEmpty()) {
            currentService = null
            updateUsageInfo(null)
            setErrorText(message("deepl.config.dialog.message.enter.auth.key"), authKeyField)
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
                logger.w("Failed to get usage info.", error)
                dialog.postUsageInfo(service, null, error)
                dialogRef.disposeSelf()
            }
    }

    private fun postUsageInfo(service: DeeplService, usage: DeeplService.Usage?, throwable: Throwable? = null) {
        if (service.authKey != currentService?.authKey) {
            return
        }

        updateUsageInfo(usage, service.isFreeAccount)

        if (throwable != null) {
            if (throwable is HttpRequests.HttpStatusException && throwable.statusCode == 403) {
                setErrorText(message("error.invalid.authentication.key"), authKeyField)
            } else {
                val message = (throwable as? IOException)
                    ?.getCommonMessage()
                    ?: throwable.message
                    ?: message("error.unknown")
                setErrorText(message)
            }
        } else {
            setErrorText(null)
        }
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater { doGetUsageInfo() }
        super.show()
    }

    override fun getHelpId(): String = HelpTopic.DEEPL.id

    override fun isOK(): Boolean {
        return DeeplCredential.isAuthKeySet
    }

    override fun doOKAction() {
        DeeplCredential.authKey = authKey
        super.doOKAction()
    }

    companion object {
        private val ERROR_FOREGROUND_COLOR = UIUtil.getErrorForeground()
        private val WARNING_FOREGROUND_COLOR = JBColor(Color(0xFB8C00), Color(0xF0A732))
    }
}
