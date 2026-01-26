package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.openai.*
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.ui.util.CredentialEditor
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.GotItTooltip
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
        get() = ui.apiEndpointField.text?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
        set(value) {
            val fixedValue = value?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
            ui.apiEndpointField.text = fixedValue
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
        ui.apiPathField.text = openAiState.apiPath
        ui.apiPathField.emptyText.text = OPEN_AI_API_PATH
        ui.azureDeploymentField.text = settings.azure.deployment.orEmpty()
        ui.azureApiVersionComboBox.selected = settings.azure.apiVersion
    }

    private fun initForTTS() {
        title = message("openai.settings.dialog.title.tts")
        ui.apiPathField.text = openAiState.ttsApiPath
        ui.apiPathField.emptyText.text = OPEN_AI_SPEECH_API_PATH
        ui.azureDeploymentField.text = azureState.ttsDeployment.orEmpty()
        ui.azureApiVersionComboBox.selected = azureState.ttsApiVersion
        ui.ttsApiSettingsTypeComboBox.selected = if (openAiState.useSeparateTtsApiSettings) {
            OpenAISettingsUI.TtsApiSettingsType.SEPARATE
        } else {
            OpenAISettingsUI.TtsApiSettingsType.SAME_AS_TRANSLATOR
        }
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

        ui.apiEndpointField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                apiEndpointChanged()
                verify(ui.apiEndpointField)
            }
        })

        ui.apiPathField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val path = ui.apiPathField.text?.trim()?.takeIf { it.isNotBlank() }
                when (configType) {
                    ConfigType.TRANSLATOR -> openAiState.apiPath = path
                    ConfigType.TTS -> openAiState.ttsApiPath = path
                }
                verify(ui.apiPathField)
            }
        })

        ui.modelComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                when (configType) {
                    ConfigType.TRANSLATOR -> openAiState.model =
                        it.item as? OpenAiGPTModel ?: OpenAiGPTModel.values().first()

                    ConfigType.TTS -> {
                        commonStates.ttsModel = it.item as? OpenAiTTSModel ?: OpenAiTTSModel.values().first()
                        updateVoicesAndFixSelection()
                    }
                }
            }
        }

        ui.customModelCheckbox.addItemListener {
            val selected = it.stateChange == ItemEvent.SELECTED
            openAiState.useCustomModel = selected
            if (configType == ConfigType.TRANSLATOR && provider == ServiceProvider.OpenAI) {
                ui.customModelField.isVisible = selected
                ui.modelComboBox.isVisible = !selected
                // To fix: https://github.com/YiiGuxing/TranslationPlugin/issues/6322
                pack()
            }
        }

        ui.customModelField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val text = ui.customModelField.text.takeUnless { it.isNullOrBlank() }?.trim()
                openAiState.customModel = text
                verify(ui.customModelField)
            }
        })

        ui.ttsApiSettingsTypeComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED && provider == ServiceProvider.OpenAI) {
                val useSeparate = it.item == OpenAISettingsUI.TtsApiSettingsType.SEPARATE
                if (openAiState.useSeparateTtsApiSettings != useSeparate) {
                    openAiState.useSeparateTtsApiSettings = useSeparate
                    ui.apiEndpointField.isEnabled = useSeparate
                    updateApiKeyEditor()
                    updateApiEndpoint()
                }
            }
        }

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
        installValidator(ui.providerComboBox) {
            when (it.selected) {
                ServiceProvider.Azure -> ValidationInfo(
                    message("openai.settings.dialog.azure.tips"),
                    ui.azureDeploymentField
                ).asWarning().withOKEnabled()

                else -> null
            }
        }

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
            when (provider) {
                ServiceProvider.OpenAI -> {
                    if (!endpoint.isValidUrl(canBeNullAndEmpty = true, withoutPaths = true)) {
                        val messageSuffix = message("openai.settings.dialog.error.invalid.endpoint.no.path")
                        val message = message("openai.settings.dialog.error.invalid.endpoint", messageSuffix)
                        ValidationInfo(message, it)
                    } else null
                }

                ServiceProvider.Azure -> {
                    if (endpoint.isNullOrEmpty()) {
                        ValidationInfo(
                            message("openai.settings.dialog.error.missing.endpoint"),
                            it
                        ).asWarning().withOKEnabled()
                    } else if (!endpoint.isValidUrl(canBeNullAndEmpty = false, withoutPaths = false)) {
                        val message = message("openai.settings.dialog.error.invalid.endpoint", "")
                        ValidationInfo(message, it)
                    } else null
                }
            }
        }

        installValidator(ui.apiPathField) {
            if (it.text.isValidPath()) null else ValidationInfo(
                message("openai.settings.dialog.error.invalid.endpoint", ""),
                it
            )
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
        showTipsIfAzureChecked()
    }

    private fun updateUiComponents() {
        val isAzure = provider == ServiceProvider.Azure
        if (isAzure) {
            ui.apiEndpointField.emptyText.text = ""
        } else {
            ui.apiEndpointField.emptyText.text = DEFAULT_OPEN_AI_API_ENDPOINT
        }

        updateApiEndpoint()
        if (configType == ConfigType.TTS) {
            ui.modelComboBox.selected = commonStates.ttsModel
            updateVoicesAndFixSelection()
            ui.ttsSpeedSlicer.value = commonStates.ttsSpeed
            ui.apiEndpointField.isEnabled = isAzure ||
                    ui.ttsApiSettingsTypeComboBox.selected === OpenAISettingsUI.TtsApiSettingsType.SEPARATE
        } else {
            ui.apiEndpointField.isEnabled = true
        }

        val componentType = when {
            isAzure -> OpenAISettingsUI.ComponentType.AZURE
            else -> OpenAISettingsUI.ComponentType.OPEN_AI
        }
        ui.showComponents(componentType)
    }

    private fun updateVoicesAndFixSelection() {
        val currentVoice = commonStates.ttsVoice
        val supportedVoices = commonStates.ttsModel.getSupportedVoices()
        if (currentVoice !in supportedVoices) {
            commonStates.ttsVoice = supportedVoices.first()
        }
        ui.ttsVoiceComboBox.model = CollectionComboBoxModel(supportedVoices, commonStates.ttsVoice)
    }

    private fun showTipsIfAzureChecked() {
        if (provider == ServiceProvider.Azure) {
            val id = TranslationPlugin.generateId("tooltip.openai.azure")
            val message = message("openai.settings.dialog.azure.tips")
            GotItTooltip(id, message, disposable)
                .withShowCount(Int.MAX_VALUE)
                .show(ui.providerComboBox, GotItTooltip.BOTTOM_MIDDLE)
        }
    }

    private fun updateApiKeyEditor() {
        currentApiKeyEditor?.stopEditing()
        currentApiKeyEditor = when (provider) {
            ServiceProvider.OpenAI -> if (configType == ConfigType.TTS && openAiState.useSeparateTtsApiSettings) {
                openAiTTSApiKeyEditor
            } else {
                openAiApiKeyEditor.apply { enabled = configType == ConfigType.TRANSLATOR }
            }

            ServiceProvider.Azure -> azureApiKeyEditor
        }
        currentApiKeyEditor?.startEditing(ui.apiKeyField)
    }

    private fun updateApiEndpoint() {
        apiEndpoint = when {
            configType == ConfigType.TRANSLATOR ||
                    provider == ServiceProvider.Azure ||
                    !openAiState.useSeparateTtsApiSettings -> commonStates.endpoint

            else -> openAiState.ttsEndpoint
        }
    }

    private fun apiEndpointChanged() {
        val endpoint = apiEndpoint
        if (!endpoint.isValidUrl(canBeNullAndEmpty = true, withoutPaths = provider == ServiceProvider.OpenAI)) {
            return
        }
        if (configType == ConfigType.TTS && openAiState.useSeparateTtsApiSettings) {
            openAiState.ttsEndpoint = endpoint
        } else {
            commonStates.endpoint = endpoint
        }
    }


    private fun verify(): Boolean {
        var valid = true
        listOf(
            ui.providerComboBox,
            ui.customModelField,
            ui.apiKeyField,
            ui.apiEndpointField,
            ui.azureDeploymentField
        ).forEach {
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
        val URL_WITHOUT_PATH_REGEX = "^https?://[^/?#\\s]+$".toRegex()
        val PATH_REGEX = "^/[^?#;\\s]*$".toRegex()

        fun String?.isValidUrl(canBeNullAndEmpty: Boolean, withoutPaths: Boolean): Boolean {
            return this?.takeIf { it.isNotEmpty() }
                ?.let { (if (withoutPaths) URL_WITHOUT_PATH_REGEX else URL_REGEX).matches(it) }
                ?: canBeNullAndEmpty
        }

        fun String?.isValidPath(): Boolean {
            return this?.takeIf { it.isNotEmpty() }?.let { PATH_REGEX.matches(it) } ?: true
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
