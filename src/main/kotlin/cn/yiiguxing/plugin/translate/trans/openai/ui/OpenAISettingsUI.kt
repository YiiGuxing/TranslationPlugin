package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.trans.openai.AzureServiceVersion
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiModel
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiTtsVoice
import cn.yiiguxing.plugin.translate.trans.openai.ServiceProvider
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JSlider

internal interface OpenAISettingsUI {

    val component: JComponent

    val providerComboBox: ComboBox<ServiceProvider>

    val apiKeyField: JBPasswordField

    val apiEndpointField: ExtendableTextField

    val azureDeploymentField: JBTextField

    val modelComboBox: ComboBox<OpenAiModel>

    val customModelField: JBTextField

    val customModelCheckbox: JCheckBox

    val azureApiVersionComboBox: ComboBox<AzureServiceVersion>

    val ttsVoiceComboBox: ComboBox<OpenAiTtsVoice>

    val ttsSpeedSlicer: JSlider

    fun showComponents(type: ComponentType)

    enum class ComponentType {
        OPEN_AI,
        AZURE
    }
}