package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel

class OpenAIConfigurationDialog : DialogWrapper(false) {

    private val apiKeyField: JBPasswordField = JBPasswordField()

    private val apiModelComboBox: ComboBox<OpenAIModel> =
        ComboBox(CollectionComboBoxModel(OpenAIModel.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.value
            }
        }

    private var apiKey: String?
        get() = apiKeyField.password
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it) }
        set(value) {
            apiKeyField.text = if (value.isNullOrEmpty()) null else value
        }


    init {
        title = message("openai.config.dialog.title")
        setResizable(false)
        init()

        // TODO Get api model from settings

        apiKey = OpenAICredential.apiKey
    }


    override fun createCenterPanel(): JComponent {
        return JPanel(VerticalLayout(JBUIScale.scale(4))).apply {
            add(createLogoPane())
            add(createConfigPanel())
        }
    }

    private fun createLogoPane(): JComponent {
        return JLabel(TranslationIcons.load("/image/openai_logo.svg")).apply {
            border = JBUI.Borders.empty(10, 0, 18, 0)
        }
    }

    private fun createConfigPanel(): JPanel {
        return JPanel(UI.migLayout("${JBUIScale.scale(8)}")).apply {
            add(JLabel(message("openai.config.dialog.label.model")))
            add(apiModelComboBox, UI.wrap())
            add(JLabel(message("openai.config.dialog.label.api.key")))
            add(apiKeyField, UI.fillX().wrap())
            add(createHintPane(), UI.fillX().cell(1, 2).wrap())
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
        text = message("openai.config.dialog.hint")
        preferredSize = JBUI.size(300, -1)
        minimumSize = JBUI.size(300, 40)
        maximumSize = JBUI.size(300, Int.MAX_VALUE)

        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean {
        return OpenAICredential.isApiKeySet
    }

    override fun doOKAction() {
        // TODO Save api model to settings

        OpenAICredential.apiKey = apiKey
        super.doOKAction()
    }
}
