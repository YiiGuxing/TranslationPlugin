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
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.Alarm
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

class OpenAISettingsDialog : DialogWrapper(false) {

    private val settings = service<OpenAISettings>()
    private val openAiState = OpenAISettings.OpenAI().apply { copyFrom(settings.openAi) }
    private val azureState = OpenAISettings.Azure().apply { copyFrom(settings.azure) }
    private val apiKeys: ApiKeys = ApiKeys()
    private var isApiKeySet: Boolean = false

    private val alarm: Alarm = Alarm(disposable)

    private val apiKeyField: JBPasswordField = JBPasswordField().apply {
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                apiKeys[provider] = String(password)
            }
        })
    }
    private val apiEndpointField: ExtendableTextField = ExtendableTextField().apply {
        emptyText.text = DEFAULT_OPEN_AI_API_ENDPOINT
        val extension = Extension.create(AllIcons.General.Reset, message("set.as.default.action.name")) {
            text = null
            setErrorText(null)
            getButton(okAction)?.requestFocus()
        }
        document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                apiEndpointChanged()
                if (apiEndpoint.isNullOrEmpty()) {
                    removeExtension(extension)
                } else if (provider == ServiceProvider.OpenAI) {
                    addExtension(extension)
                }
            }
        })
    }

    private val apiServiceProviderComboBox: ComboBox<ServiceProvider> =
        ComboBox(CollectionComboBoxModel(ServiceProvider.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.name
                label.icon = getProviderIcon(model)
            }
            addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    providerUpdated(event.item as ServiceProvider)
                }
            }
        }
    private val apiModelComboBox: ComboBox<OpenAIModel> =
        ComboBox(CollectionComboBoxModel(OpenAIModel.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.modelName
            }
            addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    currentState.model = it.item as OpenAIModel
                }
            }
        }
    private val azureServiceVersionComboBox: ComboBox<AzureServiceVersion> =
        ComboBox(CollectionComboBoxModel(AzureServiceVersion.values().toList())).apply {
            isVisible = false
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.value
            }
            addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    azureState.apiVersion = it.item as AzureServiceVersion
                }
            }
        }

    private val apiVersionLabel = JLabel(message("openai.settings.dialog.label.api.version")).apply {
        isVisible = false
    }
    private lateinit var hintComponent: JTextComponent

    private var provider: ServiceProvider
        get() = apiServiceProviderComboBox.selected ?: ServiceProvider.OpenAI
        set(value) {
            apiServiceProviderComboBox.selectedItem = value
        }

    private val currentState: OpenAISettings.OpenAI
        get() = when (provider) {
            ServiceProvider.OpenAI -> openAiState
            ServiceProvider.Azure -> azureState
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

        provider = settings.provider
        azureServiceVersionComboBox.selectedItem = settings.azure.apiVersion

        providerUpdated(settings.provider)
    }


    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/openai_logo.svg")
        val content = createConfigurationPanel()
        return LogoHeaderPanel(logo, content)
    }

    private fun createConfigurationPanel(): JPanel {
        val fieldWidth = 320
        hintComponent = UI.createHint("", fieldWidth, apiKeyField)
        return JPanel(UI.migLayout(lcBuilder = { hideMode(2) })).apply {
            val gapCC = UI.cc().gapRight(migSize(8))
            val comboBoxCC = UI.cc().width(migSize((fieldWidth * 0.6).toInt())).wrap()
            add(JLabel(message("openai.settings.dialog.label.api.provider")), gapCC)
            add(apiServiceProviderComboBox, comboBoxCC)
            add(JLabel(message("openai.settings.dialog.label.api.model")), gapCC)
            add(apiModelComboBox, comboBoxCC)
            add(apiVersionLabel, gapCC)
            add(azureServiceVersionComboBox, comboBoxCC)
            add(JLabel(message("openai.settings.dialog.label.api.endpoint")), gapCC)
            add(apiEndpointField, UI.fillX().wrap())
            add(JLabel(message("openai.settings.dialog.label.api.key")), gapCC)
            add(apiKeyField, UI.fillX().minWidth(migSize(fieldWidth)).wrap())
            add(hintComponent, UI.cc().cell(1, 5).wrap())
        }
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean {
        return isApiKeySet && currentState.endpoint.isValidEndpoint(provider == ServiceProvider.OpenAI)
    }

    private fun getHint(provider: ServiceProvider): String {
        return when (provider) {
            ServiceProvider.OpenAI -> message("openai.settings.dialog.hint")
            ServiceProvider.Azure -> message("openai.settings.dialog.hint.azure")
        }
    }

    private fun getProviderIcon(provider: ServiceProvider): Icon {
        return when (provider) {
            ServiceProvider.OpenAI -> TranslationIcons.Engines.OpenAI
            ServiceProvider.Azure -> AllIcons.Providers.Azure
        }
    }

    private fun providerUpdated(newProvider: ServiceProvider) {
        val isAzure = newProvider == ServiceProvider.Azure
        apiVersionLabel.isVisible = isAzure
        azureServiceVersionComboBox.isVisible = isAzure
        hintComponent.text = getHint(newProvider)

        if (isAzure) {
            apiEndpointField.setExtensions(emptyList())
            apiEndpointField.emptyText.text = ""
        } else {
            apiEndpointField.emptyText.text = DEFAULT_OPEN_AI_API_ENDPOINT
        }

        currentState.let {
            apiModelComboBox.selected = it.model
            apiEndpoint = it.endpoint
        }
        apiKeyField.text = apiKeys[newProvider]

        verifyApiEndpoint()
    }

    private fun apiEndpointChanged() {
        alarm.cancelAllRequests()
        alarm.addRequest(::verifyApiEndpoint, 300)

        val endpoint = apiEndpoint
        currentState.endpoint = if (endpoint.isValidEndpoint(provider == ServiceProvider.OpenAI)) {
            endpoint
        } else {
            settings.getOptions(provider).endpoint
        }
    }

    private fun verifyApiEndpoint(): Boolean {
        if (apiEndpoint.isValidEndpoint(provider == ServiceProvider.OpenAI)) {
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

        OpenAICredentials.manager(ServiceProvider.OpenAI).credential = apiKeys.openAi
        OpenAICredentials.manager(ServiceProvider.Azure).credential = apiKeys.azure

        val oldProvider = settings.provider
        val newProvider = provider
        if (oldProvider != newProvider ||
            openAiState.model != settings.openAi.model ||
            azureState.model != settings.azure.model
        ) {
            service<CacheService>().removeMemoryCache { key, _ ->
                key.translator == TranslationEngine.OPEN_AI.id
            }
        }

        settings.provider = newProvider
        settings.openAi.copyFrom(openAiState)
        settings.azure.copyFrom(azureState)
        isApiKeySet = OpenAICredentials.manager(provider).isCredentialSet

        super.doOKAction()
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater {
            val dialogRef = DisposableRef.create(disposable, this)
            runAsync {
                ApiKeys(
                    OpenAICredentials.manager(ServiceProvider.OpenAI).credential,
                    OpenAICredentials.manager(ServiceProvider.Azure).credential
                )
            }
                .expireWith(disposable)
                .successOnUiThread(dialogRef) { dialog, apiKeys ->
                    dialog.apiKeys.copyFrom(apiKeys)
                    dialog.apiKeyField.text = apiKeys[dialog.provider]
                    dialog.isApiKeySet = !apiKeys[dialog.provider].isNullOrEmpty()
                }
                .disposeAfterProcessing(dialogRef)
        }

        super.show()
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()

        fun String?.isValidEndpoint(canBeNull: Boolean = true): Boolean {
            return this?.let { URL_REGEX.matches(it) } ?: canBeNull
        }
    }

    private data class ApiKeys(var openAi: String? = null, var azure: String? = null) {

        operator fun get(provider: ServiceProvider): String? {
            return when (provider) {
                ServiceProvider.OpenAI -> openAi
                ServiceProvider.Azure -> azure
            }
        }

        operator fun set(provider: ServiceProvider, value: String?) {
            when (provider) {
                ServiceProvider.OpenAI -> openAi = value
                ServiceProvider.Azure -> azure = value
            }
        }

        fun copyFrom(apiKeys: ApiKeys) {
            openAi = apiKeys.openAi
            azure = apiKeys.azure
        }
    }
}
