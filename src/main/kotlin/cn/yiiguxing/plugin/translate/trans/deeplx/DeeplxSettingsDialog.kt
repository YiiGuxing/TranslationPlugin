package cn.yiiguxing.plugin.translate.trans.deeplx

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.google.DEFAULT_GOOGLE_API_SERVER_URL
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DeeplxSettingsDialog : DialogWrapper(false) {

    companion object {
        private val ERROR_FOREGROUND_COLOR = UIUtil.getErrorForeground()
        private val WARNING_FOREGROUND_COLOR = JBColor(Color(0xFB8C00), Color(0xF0A732))
    }

    private val settings = service<DeeplxSettings>()

    private val apiEndpointField = JBTextField().apply {
        emptyText.text = DEFAULT_GOOGLE_API_SERVER_URL
    }

    private var apiEndpoint: String?
        get() = apiEndpointField.text?.takeIf { it.isNotBlank() }
        set(value) {
            apiEndpointField.text = value?.trim() ?: ""
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
        val authKeyFieldWidth = 300
        return JPanel(UI.migLayout(UI.migSize(8))).apply {
            add(JLabel(message("deeplx.settings.dialog.label.api.endpoint")))
            add(apiEndpointField, UI.fillX().width(UI.migSize(authKeyFieldWidth)).wrap())
        }
    }


    override fun getHelpId(): String = HelpTopic.DEEPL.id

    override fun isOK(): Boolean = settings.apiEndpoint != null

    override fun doOKAction() {
        settings.apiEndpoint = apiEndpoint
        super.doOKAction()
    }
}
