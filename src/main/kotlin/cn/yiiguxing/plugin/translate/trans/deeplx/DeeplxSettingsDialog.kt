package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import icons.TranslationIcons
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DeeplxSettingsDialog : DialogWrapper(false) {

    private val settings = service<DdeeplxSettings>()

    private val apiEndpointField: JBTextField = JBTextField()

    private var apiEndpoint: String?
        get() = apiEndpointField.text?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            apiEndpointField.text = if (value.isNullOrEmpty()) null else value
        }


    init {
        title = message("deeplx.settings.dialog.title")
        isResizable = false
        init()

        apiEndpoint = settings.apiEndpoint
    }

    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/deeplx_translate_logo.svg")
        return LogoHeaderPanel(logo).apply {
            add(createAuthPanel())
        }
    }

    private fun createAuthPanel(): JPanel {
        val fieldWidth = 300
        return JPanel(UI.migLayout(UI.migSize(8))).apply {
            add(JLabel(message("deeplx.settings.dialog.label.api.endpoint")))
            add(apiEndpointField, UI.fillX().width(UI.migSize(fieldWidth)).wrap())
            add(
                UI.createHint(message("deeplx.settings.dialog.hint"), fieldWidth),
                UI.cc().cell(1, 1).wrap()
            )
        }
    }

    override fun getHelpId(): String = HelpTopic.DEEPLX.id

    override fun isOK(): Boolean = !settings.apiEndpoint.isNullOrEmpty()

    override fun doOKAction() {
        settings.apiEndpoint = apiEndpoint
        super.doOKAction()
    }
}
