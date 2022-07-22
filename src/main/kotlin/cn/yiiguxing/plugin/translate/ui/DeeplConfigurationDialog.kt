package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplCredentials
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel

class DeeplConfigurationDialog : DialogWrapper(false) {

    private val authKeyField: JBPasswordField = JBPasswordField()

    private var authKey: String?
        get() = authKeyField.password
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it) }
        set(value) {
            authKeyField.text = if (value.isNullOrEmpty()) null else value
        }


    init {
        title = message("deepl.config.dialog.title")
        isResizable = false
        init()

        authKey = DeeplCredentials.instance.authKey
    }

    override fun createCenterPanel(): JComponent = JPanel(UI.migLayout("${JBUIScale.scale(8)}!")).apply root@{
        val logo = JLabel(TranslationIcons.load("/image/deepl_translate_logo.svg"))
        logo.border = JBUI.Borders.empty(10, 0, 18, 0)
        add(logo, UI.wrap().span(2).alignX("50%"))

        add(JLabel(message("deepl.config.dialog.label.auth.key")))
        add(authKeyField, UI.fillX().wrap())

        val hintPane: JEditorPane = JEditorPane().apply {
            isEditable = false
            isFocusable = false
            background = this@root.background
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = font.deriveFont((font.size - 1).toFloat())
            editorKit = UIUtil.getHTMLEditorKit()
            border = JBUI.Borders.empty(2, 0, 0, 0)
            text = message("deepl.config.dialog.hint")
            preferredSize = JBUI.size(300, -1)
            minimumSize = JBUI.size(300, 40)
            maximumSize = JBUI.size(300, Int.MAX_VALUE)

            addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
        }
        add(hintPane, UI.fillX().cell(1, 2))
    }

    override fun getHelpId(): String = HelpTopic.DEEPL.id

    override fun isOK(): Boolean {
        return DeeplCredentials.instance.isAuthKeySet
    }

    override fun doOKAction() {
        DeeplCredentials.instance.authKey = authKey
        super.doOKAction()
    }
}