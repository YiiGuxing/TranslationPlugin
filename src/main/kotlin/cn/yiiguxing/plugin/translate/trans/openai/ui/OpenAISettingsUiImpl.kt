package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.*
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import java.util.*
import javax.swing.*

private const val MIN_WIDTH = 450

private const val OPENAI_API_KEY_PAGE_URL = "https://platform.openai.com/account/api-keys"

private fun maxWidth(vararg components: JComponent): Int = components.maxOf {
    it.setSize(10000, 1000)
    it.preferredSize.width
}


internal class OpenAISettingsUiImpl(private val configType: ConfigType) : OpenAISettingsUI {

    private val form: JComponent = JPanel(
        UI.migLayout(
            gapX = UI.migSize(8),
            gapY = UI.migSize(2),
            insets = UI.migInsets(0, 8, 0, 4),
            lcBuilder = { hideMode(3) }
        )
    )

    override val component: JComponent = LogoHeaderPanel(TranslationIcons.load("image/openai_logo.svg"), form).apply {
        minimumSize = JBUI.size(MIN_WIDTH, 0)
    }

    override val providerComboBox: ComboBox<ServiceProvider> =
        ComboBox(CollectionComboBoxModel(ServiceProvider.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.name
                label.icon = getProviderIcon(model)
            }
        }

    override val apiKeyField: JBPasswordField = JBPasswordField().apply { isEnabled = false }

    override val apiEndpointField: ExtendableTextField = ExtendableTextField()

    private val modelLabel = JLabel(message("openai.settings.dialog.label.model"))
    override val modelComboBox: ComboBox<OpenAiModel> = ComboBox<OpenAiModel>().apply {
        val models = when (configType) {
            ConfigType.TRANSLATOR -> OpenAiGPTModel.values().toList()
            ConfigType.TTS -> OpenAiTTSModel.values().toList()
        }
        model = CollectionComboBoxModel(models)
        renderer = SimpleListCellRenderer.create { label, model, _ ->
            label.text = model.modelName
        }
    }
    override val customModelField: JBTextField = JBTextField()
    override val customModelCheckbox: JCheckBox = JCheckBox(message("openai.settings.dialog.checkbox.custom.model"))
    private val modelRowWrapHolder: JPanel = JPanel().apply { isVisible = false }

    private val azureApiVersionLabel =
        JLabel(message("openai.settings.dialog.label.api.version")).apply { isVisible = false }
    override val azureApiVersionComboBox: ComboBox<AzureServiceVersion> = ComboBox<AzureServiceVersion>().apply {
        val versions = when (configType) {
            ConfigType.TRANSLATOR -> AzureServiceVersion.values().toList()
            ConfigType.TTS -> AzureServiceVersion.previewVersions()
        }
        model = CollectionComboBoxModel(versions)
        renderer = SimpleListCellRenderer.create { label, model, _ ->
            label.text = model.value
        }
    }

    private val azureDeploymentLabel =
        JLabel(message("openai.settings.dialog.label.deployment"))
    override val azureDeploymentField: JBTextField = JBTextField()

    override lateinit var ttsVoiceComboBox: ComboBox<OpenAiTtsVoice>
        private set
    override lateinit var ttsSpeedSlicer: JSlider
        private set


    private val apiKeyHelpLabel: JComponent =
        ContextHelpLabel.createWithLink(
            null,
            message("openai.settings.dialog.openai.api.key.help"),
            message("openai.settings.dialog.openai.api.key.help.link.text")
        ) {
            BrowserUtil.open(OPENAI_API_KEY_PAGE_URL)
        }
    private val endpointRowWrapHolder: JComponent = JLabel()
    private val azureDeploymentHelpLabel: JComponent =
        ContextHelpLabel.createWithLink(
            null,
            message("openai.settings.dialog.azure.deployment.help"),
            message("link.learn.more")
        ) {
            BrowserUtil.open(message("openai.settings.dialog.azure.deployment.help.url"))
        }
    private val azureApiKeyHelpLabel: JComponent = azureHelpLabel()
    private val azureEndpointHelpLabel: JComponent = azureHelpLabel()


    init {
        if (configType == ConfigType.TTS) {
            initTtsComponents()
        }
        layout()
    }

    private fun azureHelpLabel() = ContextHelpLabel.createWithLink(
        null,
        message("openai.settings.dialog.azure.api.key.help"),
        message("link.learn.more")
    ) {
        val url = when (configType) {
            ConfigType.TRANSLATOR -> message("openai.settings.dialog.azure.api.key.help.url")
            ConfigType.TTS -> message("openai.settings.dialog.azure.tts.api.key.help.url")
        }
        BrowserUtil.open(url)
    }

    private fun initTtsComponents() {
        ttsVoiceComboBox = ComboBox(OpenAiTtsVoice.values()).apply {
            renderer = SimpleListCellRenderer.create { label, voice, _ ->
                label.text = voice.voiceName
            }
        }
        ttsSpeedSlicer = JSlider(OpenAiTTSSpeed.MIN, OpenAiTTSSpeed.MAX, OpenAiTTSSpeed.NORMAL).apply {
            majorTickSpacing = 100
            minorTickSpacing = 10
            snapToTicks = true
            paintTicks = true
            paintLabels = true
            labelTable = Hashtable<Int, JLabel>().apply {
                put(OpenAiTTSSpeed.MIN, JLabel(message("tts.label.speed.slow")))
                put(OpenAiTTSSpeed.NORMAL, JLabel(message("tts.label.speed.normal")))
                put(OpenAiTTSSpeed.MAX, JLabel(message("tts.label.speed.fast")))
            }
        }
    }

    private fun layout() {
        val isTTS = configType == ConfigType.TTS

        fun comboBoxCC() = UI.cc()
            .sizeGroupX("combo-box")
            .minWidth(UI.migSize((MIN_WIDTH * 0.5).toInt()))

        val providerLabel = JLabel(message("openai.settings.dialog.label.provider"))
        val apiKeyLabel = JLabel(message("openai.settings.dialog.label.api.key"))
        val endpointLabel = JLabel(message("openai.settings.dialog.label.endpoint"))
        val maxWidth = maxWidth(
            providerLabel,
            modelLabel,
            azureDeploymentLabel,
            apiKeyLabel,
            endpointLabel,
            azureApiVersionLabel
        )
        val labelCC = UI.cc()
            .sizeGroupX("label")
            .alignY("center")
            .minWidth(UI.migSize(maxWidth, false))

        form.add(providerLabel, labelCC)
        form.add(providerComboBox, comboBoxCC().wrap())
        form.add(modelLabel, labelCC)
        form.add(modelComboBox, comboBoxCC())
        form.add(customModelField, comboBoxCC())
        form.add(customModelCheckbox, UI.fillX().alignY("center").wrap())
        form.add(modelRowWrapHolder, UI.wrap())

        form.add(azureDeploymentLabel, labelCC)
        form.add(azureDeploymentField, UI.fillX().spanX(2))
        form.add(azureDeploymentHelpLabel, UI.wrap())
        form.add(apiKeyLabel, labelCC)
        form.add(apiKeyField, UI.fillX().spanX(2))
        form.add(apiKeyHelpLabel, UI.wrap())
        form.add(azureApiKeyHelpLabel, UI.wrap())
        form.add(endpointLabel, labelCC)
        form.add(apiEndpointField, UI.fillX().spanX(2))
        form.add(endpointRowWrapHolder, UI.wrap())
        form.add(azureEndpointHelpLabel, UI.wrap())
        form.add(azureApiVersionLabel, labelCC)
        form.add(azureApiVersionComboBox, comboBoxCC().wrap())

        if (isTTS) {
            form.add(JLabel(message("tts.label.voice")), labelCC)
            form.add(ttsVoiceComboBox, comboBoxCC().wrap())
            form.add(JLabel(message("tts.label.speed")), labelCC)
            form.add(ttsSpeedSlicer, UI.fillX().spanX(2).wrap())
        }
    }

    private fun getProviderIcon(provider: ServiceProvider): Icon {
        return when (provider) {
            ServiceProvider.OpenAI -> TranslationIcons.Engines.OpenAI
            ServiceProvider.Azure -> AllIcons.Providers.Azure
        }
    }

    override fun showComponents(type: OpenAISettingsUI.ComponentType) {
        val isOpenAI = type == OpenAISettingsUI.ComponentType.OPEN_AI
        val isAzure = type == OpenAISettingsUI.ComponentType.AZURE

        when (configType) {
            ConfigType.TRANSLATOR -> {
                modelLabel.isVisible = isOpenAI
                modelComboBox.isVisible = isOpenAI && !customModelCheckbox.isSelected
                customModelField.isVisible = isOpenAI && customModelCheckbox.isSelected
                customModelCheckbox.isVisible = isOpenAI
                modelRowWrapHolder.isVisible = false
            }

            ConfigType.TTS -> {
                modelLabel.isVisible = true
                modelComboBox.isVisible = true
                customModelField.isVisible = false
                customModelCheckbox.isVisible = false
                modelRowWrapHolder.isVisible = true
            }
        }

        apiKeyHelpLabel.isVisible = isOpenAI
        endpointRowWrapHolder.isVisible = isOpenAI
        azureDeploymentLabel.isVisible = isAzure
        azureDeploymentField.isVisible = isAzure
        azureApiVersionLabel.isVisible = isAzure
        azureApiVersionComboBox.isVisible = isAzure
        azureApiKeyHelpLabel.isVisible = isAzure
        azureEndpointHelpLabel.isVisible = isAzure
        azureDeploymentHelpLabel.isVisible = isAzure
    }
}