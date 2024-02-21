package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.openai.*
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.intellij.util.containers.orNull
import org.jetbrains.concurrency.runAsync
import java.awt.event.ItemEvent
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

class OpenAISettingsDialog : DialogWrapper(false) {

    private val settings = service<OpenAISettings>()
    private val openAiState = OpenAISettings.OpenAI().apply { copyFrom(settings.openAi) }
    private val azureState = OpenAISettings.Azure().apply { copyFrom(settings.azure) }
    private val apiKeys: ApiKeys = ApiKeys()
    private var isApiKeySet: Boolean = false

    private val ui: OpenAISettingsUI = OpenAISettingsUiImpl()

    private var provider: ServiceProvider
        get() = ui.providerComboBox.selected ?: ServiceProvider.OpenAI
        set(value) {
            ui.providerComboBox.selected = value
        }

    private var endpointState: String?
        get() = when (provider) {
            ServiceProvider.OpenAI -> openAiState.endpoint
            ServiceProvider.Azure -> azureState.endpoint
        }
        set(value) {
            when (provider) {
                ServiceProvider.OpenAI -> openAiState.endpoint = value
                ServiceProvider.Azure -> azureState.endpoint = value
            }
        }

    private var apiEndpoint: String?
        get() = ui.apiEndpointField.text?.trim()?.takeIf { it.isNotEmpty() }
        set(value) {
            ui.apiEndpointField.text = if (value.isNullOrEmpty()) null else value
        }

    init {
        title = message("openai.settings.dialog.title")
        setResizable(false)
        init()
        initListeners()
        initValidators()

        provider = settings.provider
        ui.azureApiVersionComboBox.selected = settings.azure.apiVersion
        ui.azureDeploymentField.text = settings.azure.deploymentId.orEmpty()

        providerUpdated(settings.provider)
    }

    override fun createCenterPanel(): JComponent = ui.component

    private fun initListeners() {
        ui.providerComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                providerUpdated(event.item as ServiceProvider)
            }
        }

        ui.apiKeyField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                apiKeys[provider] = String(ui.apiKeyField.password)
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

        ui.apiModelComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                openAiState.model = it.item as OpenAIModel
            }
        }

        ui.azureDeploymentField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                azureState.deploymentId = ui.azureDeploymentField.text.takeUnless { it.isNullOrBlank() }?.trim()
                verify(ui.azureDeploymentField)
            }
        })

        ui.azureApiVersionComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                azureState.apiVersion = it.item as AzureServiceVersion
            }
        }
    }

    private fun initValidators() {
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

    override fun isOK(): Boolean = isApiKeySet && settings.isConfigured

    private fun providerUpdated(newProvider: ServiceProvider) {
        val isAzure = newProvider == ServiceProvider.Azure
        if (isAzure) {
            ui.apiEndpointField.setExtensions(emptyList())
            ui.apiEndpointField.emptyText.text = ""
        } else {
            ui.apiEndpointField.emptyText.text = DEFAULT_OPEN_AI_API_ENDPOINT
        }
        ui.setOpenAiFormComponentsVisible(!isAzure)
        ui.setAzureFormComponentsVisible(isAzure)

        apiEndpoint = endpointState
        ui.apiKeyField.text = apiKeys[newProvider]

        invokeLater {
            verify()
            pack()
        }
    }

    private fun apiEndpointChanged() {
        val endpoint = apiEndpoint
        endpointState = if (endpoint.isValidEndpoint(true)) {
            endpoint
        } else {
            settings.getOptions(provider).endpoint
        }
    }

    private fun verify(component: JComponent): ValidationInfo? {
        return ComponentValidator.getInstance(component).map {
            it.revalidate()
            it.validationInfo
        }.orNull()
    }

    private fun verify(): Boolean {
        var valid = true
        var focusTarget: JComponent? = null
        listOf(ui.apiKeyField, ui.apiEndpointField, ui.azureDeploymentField).forEach {
            verify(it)?.let { info ->
                // 校验不通过的聚焦优先级最高
                if (valid && it.isShowing) {
                    focusTarget = it
                }
                valid = valid && info.okEnabled
            }
        }

        focusTarget?.requestFocus()

        return valid
    }

    override fun doOKAction() {
        if (!verify()) {
            return
        }

        OpenAICredentials.manager(ServiceProvider.OpenAI).credential = apiKeys.openAi
        OpenAICredentials.manager(ServiceProvider.Azure).credential = apiKeys.azure

        val oldProvider = settings.provider
        val newProvider = provider
        if (oldProvider != newProvider ||
            openAiState.model != settings.openAi.model ||
            azureState.deploymentId != settings.azure.deploymentId
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
                    dialog.ui.apiKeyField.text = apiKeys[dialog.provider]
                    dialog.isApiKeySet = !apiKeys[dialog.provider].isNullOrEmpty()
                    dialog.verify()
                }
                .disposeAfterProcessing(dialogRef)
        }

        super.show()
    }

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()

        fun String?.isValidEndpoint(canBeNullAndEmpty: Boolean = true): Boolean {
            return this?.takeIf { it.isNotEmpty() }?.let { URL_REGEX.matches(it) } ?: canBeNullAndEmpty
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
