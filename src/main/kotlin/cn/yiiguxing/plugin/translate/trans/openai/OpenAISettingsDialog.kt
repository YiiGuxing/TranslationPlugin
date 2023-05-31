package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.UI.migSize
import cn.yiiguxing.plugin.translate.ui.selected
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import icons.TranslationIcons
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OpenAISettingsDialog : DialogWrapper(false) {

    private val settings = service<OpenAISettings>()

    private val apiKeyField: JBPasswordField = JBPasswordField()

    private val apiModelComboBox: ComboBox<OpenAIModel> =
        ComboBox(CollectionComboBoxModel(OpenAIModel.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.modelName
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
        title = message("openai.settings.dialog.title")
        setResizable(false)
        init()

        apiKey = OpenAICredential.apiKey
        apiModelComboBox.selectedItem = settings.model
    }


    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/openai_logo.svg")
        val content = createConfigurationPanel()
        return LogoHeaderPanel(logo, content)
    }

    private fun createConfigurationPanel(): JPanel {
        val apiKeyFieldWidth = 320
        return JPanel(UI.migLayout(migSize(8))).apply {
            add(JLabel(message("openai.settings.dialog.label.model")))
            add(apiModelComboBox, UI.wrap())
            add(JLabel(message("openai.settings.dialog.label.api.key")))
            add(apiKeyField, UI.cc().width(migSize(apiKeyFieldWidth)).wrap())
            add(UI.createHint(message("openai.settings.dialog.hint"), apiKeyFieldWidth), UI.cc().cell(1, 2).wrap())
        }
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean = OpenAICredential.isApiKeySet

    override fun doOKAction() {
        OpenAICredential.apiKey = apiKey
        settings.model = apiModelComboBox.selected ?: OpenAIModel.GPT_3_5_TURBO

        super.doOKAction()
    }
}
