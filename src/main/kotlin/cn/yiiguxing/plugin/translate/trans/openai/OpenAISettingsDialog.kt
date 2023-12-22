package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.UI.migSize
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
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

    private var isOK: Boolean = false

    private var apiEndpoint: String?
        get() = apiEndpointField.text?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            apiEndpointField.text = if (value.isNullOrEmpty()) null else value
        }

    init {
        title = message("openai.settings.dialog.title")
        setResizable(false)
        init()

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
        val apiPathLabel = JBLabel(OpenAI.API_PATH).apply {
            border = JBUI.Borders.emptyRight(apiEndpointField.insets.right)
            isEnabled = false
        }
        return JPanel(UI.migLayout()).apply {
            val gapCC = UI.cc().gapRight(migSize(8))
            add(JLabel(message("openai.settings.dialog.label.model")), gapCC)
            add(apiModelComboBox, UI.fillX().wrap())
            add(JLabel(message("openai.settings.dialog.label.api.endpoint")), gapCC)
            add(apiEndpointField, UI.fillX())
            add(apiPathLabel, UI.cc().gapLeft(migSize(2)).wrap())
            add(JLabel(message("openai.settings.dialog.label.api.key")), gapCC)
            add(apiKeyField, UI.fillX().spanX(2).minWidth(migSize(fieldWidth)).wrap())
            add(
                UI.createHint(message("openai.settings.dialog.hint"), fieldWidth, apiKeyField),
                UI.cc().cell(1, 3).spanX(2).wrap()
            )
        }
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean = isOK

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
        isOK = OpenAICredential.isApiKeySet
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

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater {
            val dialogRef = DisposableRef.create(disposable, this)
            runAsync { OpenAICredential.apiKey to OpenAICredential.isApiKeySet }
                .expireWith(disposable)
                .successOnUiThread(dialogRef) { dialog, (apiKey, isApiKeySet) ->
                    dialog.apiKey = apiKey
                    dialog.isOK = isApiKeySet
                }
                .disposeAfterProcessing(dialogRef)
        }

        super.show()
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()
    }
}
