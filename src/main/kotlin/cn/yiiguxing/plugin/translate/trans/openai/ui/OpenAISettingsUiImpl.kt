package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.AzureServiceVersion
import cn.yiiguxing.plugin.translate.trans.openai.OpenAIModel
import cn.yiiguxing.plugin.translate.trans.openai.ServiceProvider
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val MAX_WIDTH = 450

internal class OpenAISettingsUiImpl : OpenAISettingsUI {

    private val form: JComponent = JPanel(UI.migLayout(gapX = UI.migSize(8), lcBuilder = { hideMode(3) }))

    override val component: JComponent = LogoHeaderPanel(TranslationIcons.load("image/openai_logo.svg"), form)

    override val providerComboBox: ComboBox<ServiceProvider> =
        ComboBox(CollectionComboBoxModel(ServiceProvider.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.name
                label.icon = getProviderIcon(model)
            }
        }

    override val apiKeyField: JBPasswordField = JBPasswordField()

    override val apiEndpointField: ExtendableTextField = ExtendableTextField()

    private val apiModelLabel = JLabel(message("openai.settings.dialog.label.api.model"))
    override val apiModelComboBox: ComboBox<OpenAIModel> =
        ComboBox(CollectionComboBoxModel(OpenAIModel.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.modelName
            }
        }

    private val azureApiVersionLabel =
        JLabel(message("openai.settings.dialog.label.api.version")).apply { isVisible = false }
    override val azureApiVersionComboBox: ComboBox<AzureServiceVersion> =
        ComboBox(CollectionComboBoxModel(AzureServiceVersion.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, model, _ ->
                label.text = model.value
            }
        }

    private val azureDeploymentLabel =
        JLabel(message("openai.settings.dialog.label.deployment"))
    override val azureDeploymentField: JBTextField = JBTextField()

    private val hint: JComponent = UI.createHint(message("openai.settings.dialog.hint"), MAX_WIDTH)
    private val azureHint: JComponent = UI.createHint( message("openai.settings.dialog.hint.azure"), MAX_WIDTH)


    init {
        layout()
    }

    private fun layout() {
        val comboboxCC = UI.wrap().width(UI.migSize((MAX_WIDTH * 0.5).toInt()))
        form.maximumSize = JBUI.size(MAX_WIDTH, Integer.MAX_VALUE)
        form.add(JLabel(message("openai.settings.dialog.label.api.provider")))
        form.add(providerComboBox, comboboxCC)
        form.add(apiModelLabel)
        form.add(apiModelComboBox, comboboxCC)
        form.add(azureApiVersionLabel)
        form.add(azureApiVersionComboBox, comboboxCC)

        val fillX = UI.fillX().wrap()
        form.add(azureDeploymentLabel)
        form.add(azureDeploymentField, fillX)
        form.add(JLabel(message("openai.settings.dialog.label.api.endpoint")))
        form.add(apiEndpointField, fillX)
        form.add(JLabel(message("openai.settings.dialog.label.api.key")))
        form.add(apiKeyField, fillX)

        val hintCC = UI.spanX(2).gapTop(UI.migSize(16)).wrap()
        form.add(hint, hintCC)
        form.add(azureHint, hintCC)
    }

    private fun getProviderIcon(provider: ServiceProvider): Icon {
        return when (provider) {
            ServiceProvider.OpenAI -> TranslationIcons.Engines.OpenAI
            ServiceProvider.Azure -> AllIcons.Providers.Azure
        }
    }

    override fun setOpenAiFormComponentsVisible(visible: Boolean) {
        apiModelLabel.isVisible = visible
        apiModelComboBox.isVisible = visible
        hint.isVisible = visible
    }

    override fun setAzureFormComponentsVisible(visible: Boolean) {
        azureDeploymentLabel.isVisible = visible
        azureDeploymentField.isVisible = visible
        azureApiVersionLabel.isVisible = visible
        azureApiVersionComboBox.isVisible = visible
        azureHint.isVisible = visible
    }
}