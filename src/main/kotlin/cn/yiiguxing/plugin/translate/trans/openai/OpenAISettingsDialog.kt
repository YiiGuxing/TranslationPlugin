package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.UI.migSize
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.Alarm
import icons.TranslationIcons
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class OpenAISettingsDialog : DialogWrapper(false) {

    private val settings = service<OpenAISettings>()
    private val alarm: Alarm = Alarm(disposable)

    private val apiKeyField: JBPasswordField = JBPasswordField()
    private val apiEndpointField: ExtendableTextField = ExtendableTextField().apply {
        emptyText.text = OpenAI.DEFAULT_API_ENDPOINT
        val extension = Extension.create(AllIcons.General.Reset, message("set.as.default.action.name")) {
            text = null
            setErrorText(null)
            getButton(okAction)?.requestFocus()
        }
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                alarm.cancelAllRequests()
                alarm.addRequest(::verifyApiEndpoint, 300)
                if (apiEndpoint.isNullOrEmpty()) {
                    removeExtension(extension)
                } else {
                    addExtension(extension)
                }
            }
        })
    }

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

    private var apiEndpoint: String?
        get() = apiEndpointField.text?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            apiEndpointField.text = if (value.isNullOrEmpty()) null else value
        }

    init {
        title = message("openai.settings.dialog.title")
        setResizable(false)
        init()

        apiKey = OpenAICredential.apiKey
        apiEndpoint = settings.apiEndpoint
        apiModelComboBox.selectedItem = settings.model
    }


    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/openai_logo.svg")
        val content = createConfigurationPanel()
        return LogoHeaderPanel(logo, content)
    }

    private fun createConfigurationPanel(): JPanel {
        val fieldWidth = 320
        return JPanel(UI.migLayout(migSize(8))).apply {
            add(JLabel(message("openai.settings.dialog.label.model")))
            add(apiModelComboBox, UI.wrap())
            add(JLabel(message("openai.settings.dialog.label.api.endpoint")))
            add(apiEndpointField, UI.cc().width(migSize(fieldWidth)).wrap())
            add(JLabel(message("openai.settings.dialog.label.api.key")))
            add(apiKeyField, UI.cc().width(migSize(fieldWidth)).wrap())
            add(UI.createHint(message("openai.settings.dialog.hint"), fieldWidth), UI.cc().cell(1, 3).wrap())
        }
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean = OpenAICredential.isApiKeySet

    private fun verifyApiEndpoint(): Boolean {
        if (apiEndpoint.let { it == null || URL_REGEX.matches(it) }) {
            setErrorText(null)
            return true
        }

        setErrorText(message("openai.settings.dialog.error.invalid.api.endpoint"), apiEndpointField)
        return false
    }

    override fun doOKAction() {
        if (!verifyApiEndpoint()) {
            return
        }

        OpenAICredential.apiKey = apiKey
        settings.apiEndpoint = apiEndpoint

        val oldModel = settings.model
        val newModel = apiModelComboBox.selected ?: OpenAIModel.GPT_3_5_TURBO
        if (oldModel != newModel) {
            settings.model = newModel
            service<CacheService>().removeMemoryCache { key, _ ->
                key.translator == TranslationEngine.OPEN_AI.id
            }
        }

        super.doOKAction()
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()
    }
}
