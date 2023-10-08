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
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class OpenAISettingsDialog : DialogWrapper(false) {

    private val settings = service<OpenAISettings>()

    private val apiKeyField: JBPasswordField = JBPasswordField()

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

    init {
        title = message("openai.settings.dialog.title")
        setResizable(false)
        init()

        apiModelComboBox.selectedItem = settings.model
    }


    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("image/openai_logo.svg")
        val content = createConfigurationPanel()
        return LogoHeaderPanel(logo, content)
    }

    private fun createConfigurationPanel(): JPanel {
        val apiKeyFieldWidth = 320
        return JPanel(UI.migLayout(migSize(8))).apply {
            add(JLabel(message("openai.settings.dialog.label.model")))
            add(apiModelComboBox, UI.wrap())
            add(JLabel(message("openai.settings.dialog.label.api.key")))
            add(apiKeyField, UI.cc().width(migSize(apiKeyFieldWidth)).wrap())
            add(
                UI.createHint(message("openai.settings.dialog.hint"), apiKeyFieldWidth, apiKeyField),
                UI.cc().cell(1, 2).wrap()
            )
        }
    }

    override fun getHelpId(): String = HelpTopic.OPEN_AI.id

    override fun isOK(): Boolean = isOK

    override fun doOKAction() {
        OpenAICredential.apiKey = apiKey
        isOK = OpenAICredential.isApiKeySet

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
}
