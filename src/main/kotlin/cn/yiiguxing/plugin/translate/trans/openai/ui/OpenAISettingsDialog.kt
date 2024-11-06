package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.openai.*
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.ui.util.CredentialEditor
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.intellij.util.containers.orNull
import java.awt.event.ItemEvent
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

class OpenAISettingsDialog(private val configType: ConfigType) : DialogWrapper(false) {

    private val settings = service<OpenAiSettings>()
    private val openAiState = OpenAiSettings.OpenAi().apply { copyFrom(settings.openAi) }
    private val azureState = OpenAiSettings.Azure().apply { copyFrom(settings.azure) }

    private val openAiApiKeyEditor = createAiApiKeyEditor(ServiceProvider.OpenAI)
    private val openAiTTSApiKeyEditor = createAiApiKeyEditor(ServiceProvider.OpenAI, true)
    private val azureApiKeyEditor = createAiApiKeyEditor(ServiceProvider.Azure)
    private var currentApiKeyEditor: CredentialEditor? = null

    private val ui: OpenAISettingsUI = OpenAISettingsUiImpl(configType)

    private var provider: ServiceProvider
        get() = ui.providerComboBox.selected ?: ServiceProvider.OpenAI
        set(value) {
            ui.providerComboBox.selected = value
        }

    private val commonStates: OpenAiSettings.CommonState
        get() = when (provider) {
            ServiceProvider.OpenAI -> openAiState
            ServiceProvider.Azure -> azureState
        }

    private val azureCommonState = AzureCommonState(azureState, configType)

    private var apiEndpoint: String?
        get() = ui.apiEndpointField.text?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            ui.apiEndpointField.text = if (value.isNullOrEmpty()) null else value
        }

    init {
        isResizable = false
        init()
        initListeners()
        initValidators()

        when (configType) {
            ConfigType.TRANSLATOR -> initForTranslator()
            ConfigType.TTS -> initForTTS()
        }
        provider = settings.provider
        updateUiComponents()
    }

    private fun initForTranslator() {
        title = message("openai.settings.dialog.title")
        ui.modelComboBox.selected = openAiState.model
        ui.customModelField.text = openAiState.customModel
        ui.customModelCheckbox.isSelected = openAiState.useCustomModel
        ui.azureDeploymentField.text = settings.azure.deployment.orEmpty()
        ui.azureApiVersionComboBox.selected = settings.azure.apiVersion
    }

    private fun initForTTS() {
        title = message("openai.settings.dialog.title.tts")
        ui.azureDeploymentField.text = settings.azure.ttsDeployment.orEmpty()
        ui.azureApiVersionComboBox.selected = settings.azure.ttsApiVersion
    }

    override fun createCenterPanel(): JComponent = ui.component

    private fun initListeners() {
        ui.providerComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                providerUpdated()
            }
        }

        ui.apiKeyField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                verify(ui.apiKeyField)
            }
        })

        ui.apiEndpointField.apply {
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

                    verify(this@OpenAISettingsDialog.ui.apiEndpointField)
                }
            })
        }

        ui.modelComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                when (configType) {
                    ConfigType.TRANSLATOR -> openAiState.model =
                        it.item as? OpenAiGPTModel ?: OpenAiGPTModel.values().first()

                    ConfigType.TTS -> commonStates.ttsModel =
                        it.item as? OpenAiTTSModel ?: OpenAiTTSModel.values().first()
                }
            }
        }

        ui.customModelCheckbox.addItemListener {
            val selected = it.stateChange == ItemEvent.SELECTED
            openAiState.useCustomModel = selected
            if (configType == ConfigType.TRANSLATOR && provider == ServiceProvider.OpenAI) {
                ui.customModelField.isVisible = selected
                ui.modelComboBox.isVisible = !selected
            }
        }

        ui.customModelField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val text = ui.customModelField.text.takeUnless { it.isNullOrBlank() }?.trim()
                openAiState.customModel = text
                verify(ui.customModelField)
            }
        })

        ui.azureDeploymentField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                azureCommonState.deployment = ui.azureDeploymentField.text.takeUnless { it.isNullOrBlank() }?.trim()
                verify(ui.azureDeploymentField)
            }
        })

        ui.azureApiVersionComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                azureCommonState.apiVersion = it.item as AzureServiceVersion
            }
        }

        if (configType == ConfigType.TTS) {
            ui.ttsVoiceComboBox.addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    commonStates.ttsVoice = it.item as OpenAiTtsVoice
                }
            }
            ui.ttsSpeedSlicer.addChangeListener {
                commonStates.ttsSpeed = ui.ttsSpeedSlicer.value
            }
        }
    }

    private fun initValidators() {
        installValidator(ui.customModelField) {
            val customModel = it.text
            when {
                ui.customModelCheckbox.isSelected && customModel.isNullOrBlank() -> ValidationInfo(
                    message("openai.settings.dialog.error.missing.custom.model"),
                    it
                )

                else -> null
            }
        }


        installValidator(ui.apiKeyField) {
            val password = it.password
            when {
                password == null || password.isEmpty() -> ValidationInfo(
                    message("openai.settings.dialog.error.missing.api.key"),
                    it
                ).asWarning().withOKEnabled()

                else -> null
            }
        }

        installValidator(ui.apiEndpointField) {
            val endpoint = it.text
            when {
                provider != ServiceProvider.OpenAI && endpoint.isNullOrEmpty() -> ValidationInfo(
                    message("openai.settings.dialog.error.missing.endpoint"),
                    it
                ).asWarning().withOKEnabled()

                !endpoint.isValidEndpoint(provider == ServiceProvider.OpenAI) -> ValidationInfo(
                    message("openai.settings.dialog.error.invalid.endpoint"),
                    it
                )

                else -> null
            }
        }

        installValidator(ui.azureDeploymentField) {
            when {
                it.text.isNullOrBlank() -> ValidationInfo(
                    message("openai.settings.dialog.error.missing.deployment"),
                    it
                ).asWarning().withOKEnabled()

                else -> null
            }
        }
    }

    private inline fun <T : JComponent> installValidator(component: T, crossinline validator: (T) -> ValidationInfo?) {
        ComponentValidator(disposable).withValidator(Supplier { validator(component) }).installOn(component)
    }

    override fun getHelpId(): String = when (provider) {
        ServiceProvider.OpenAI -> HelpTopic.OPEN_AI.id
        ServiceProvider.Azure -> HelpTopic.AZURE_OPEN_AI.id
    }

    override fun isOK(): Boolean = currentApiKeyEditor?.isCredentialSet == true && settings.isConfigured(configType)

    private fun providerUpdated() {
        updateUiComponents()
        updateApiKeyEditor()
        verify()
        pack()
    }

    private fun updateUiComponents() {
        val isAzure = provider == ServiceProvider.Azure
        if (isAzure) {
            ui.apiEndpointField.setExtensions(emptyList())
            ui.apiEndpointField.emptyText.text = ""
        } else {
            ui.apiEndpointField.emptyText.text = DEFAULT_OPEN_AI_API_ENDPOINT
        }

        apiEndpoint = commonStates.endpoint
        if (configType == ConfigType.TTS) {
            ui.modelComboBox.selected = commonStates.ttsModel
            ui.ttsVoiceComboBox.selected = commonStates.ttsVoice
            ui.ttsSpeedSlicer.value = commonStates.ttsSpeed
        }

        val componentType = when {
            isAzure -> OpenAISettingsUI.ComponentType.AZURE
            else -> OpenAISettingsUI.ComponentType.OPEN_AI
        }
        ui.showComponents(componentType)
    }

    private fun updateApiKeyEditor() {
        currentApiKeyEditor?.stopEditing()
        currentApiKeyEditor = when (provider) {
            ServiceProvider.OpenAI -> if (configType == ConfigType.TTS && !openAiState.sameApiOptionsInTTS) {
                openAiTTSApiKeyEditor
            } else openAiApiKeyEditor

            ServiceProvider.Azure -> azureApiKeyEditor
        }
        currentApiKeyEditor?.startEditing(ui.apiKeyField)
    }

    private fun apiEndpointChanged() {
        val endpoint = apiEndpoint
            ?.takeIf { it.isValidEndpoint(true) }
            ?: settings.getOptions(provider).endpoint
        commonStates.endpoint = endpoint
    }


    private fun verify(): Boolean {
        var valid = true
        listOf(ui.customModelField, ui.apiKeyField, ui.apiEndpointField, ui.azureDeploymentField).forEach {
            verify(it)?.let { info ->
                valid = valid && info.okEnabled
            }
        }
        return valid
    }

    override fun doOKAction() {
        if (!verify()) {
            return
        }

        applyApiKeys()

        val oldProvider = settings.provider
        val newProvider = provider
        if (oldProvider != newProvider ||
            openAiState.useCustomModel != settings.openAi.useCustomModel ||
            (!openAiState.useCustomModel && openAiState.model != settings.openAi.model) ||
            (openAiState.useCustomModel && openAiState.customModel != settings.openAi.customModel) ||
            azureState.deployment != settings.azure.deployment
        ) {
            service<CacheService>().removeMemoryCache { key, _ ->
                key.translator == TranslationEngine.OPEN_AI.id
            }
        }

        settings.provider = newProvider
        settings.openAi.copyFrom(openAiState)
        settings.azure.copyFrom(azureState)

        super.doOKAction()
    }

    private fun applyApiKeys() {
        openAiApiKeyEditor.applyEditing()
        openAiTTSApiKeyEditor.applyEditing()
        azureApiKeyEditor.applyEditing()
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater {
            // Since the API Key is loaded asynchronously,
            // it needs to be loaded after the window is
            // displayed to ensure that the load completion
            // callback runs in the correct window modality state.
            updateApiKeyEditor()
            verify()
        }

        super.show()
    }

    private fun createAiApiKeyEditor(provider: ServiceProvider, forTTS: Boolean = false): CredentialEditor {
        return CredentialEditor(disposable) { OpenAiCredentials.manager(provider, forTTS) }
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()

        fun String?.isValidEndpoint(canBeNullAndEmpty: Boolean = true): Boolean {
            return this?.takeIf { it.isNotEmpty() }?.let { URL_REGEX.matches(it) } ?: canBeNullAndEmpty
        }

        private fun verify(component: JComponent): ValidationInfo? {
            return ComponentValidator.getInstance(component).map {
                it.revalidate()
                it.validationInfo
            }.orNull()
        }
    }

    private class AzureCommonState(azureState: OpenAiSettings.Azure, configType: ConfigType) {
        var deployment: String? by when (configType) {
            ConfigType.TRANSLATOR -> azureState::deployment
            ConfigType.TTS -> azureState::ttsDeployment
        }

        var apiVersion: AzureServiceVersion by when (configType) {
            ConfigType.TRANSLATOR -> azureState::apiVersion
            ConfigType.TTS -> azureState::ttsApiVersion
        }
    }
}
