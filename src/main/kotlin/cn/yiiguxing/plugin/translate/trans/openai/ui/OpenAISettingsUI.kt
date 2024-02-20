package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.trans.openai.AzureServiceVersion
import cn.yiiguxing.plugin.translate.trans.openai.OpenAIModel
import cn.yiiguxing.plugin.translate.trans.openai.ServiceProvider
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import javax.swing.JComponent

internal interface OpenAISettingsUI {

    val component: JComponent

    val providerComboBox: ComboBox<ServiceProvider>

    val apiKeyField: JBPasswordField

    val apiEndpointField: ExtendableTextField

    val azureDeploymentField: JBTextField

    val apiModelComboBox: ComboBox<OpenAIModel>

    val azureApiVersionComboBox: ComboBox<AzureServiceVersion>

    fun setOpenAiFormComponentsVisible(visible: Boolean)

    fun setAzureFormComponentsVisible(visible: Boolean)

}