package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.util.CredentialEditor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.DocumentAdapter
import javax.swing.JComponent
import javax.swing.JComboBox

internal class MicrosoftSettingsDialog : DialogWrapper(false) {

    private val settings = MicrosoftSettings.getInstance()
    private var selectedTranslator = settings.translator
    private var selectedRegion = settings.region
    private val keyEditor = CredentialEditor(disposable) {
        object : cn.yiiguxing.plugin.translate.util.credential.StringCredentialManager {
            override var credential: String?
                get() = settings.getSubscriptionKey()
                set(value) = settings.setSubscriptionKey(value)
            override val isCredentialSet: Boolean
                get() = settings.isSubscriptionKeySet
        }
    }
    private lateinit var translatorComboBox: JComboBox<MicrosoftTranslatorType>
    private lateinit var subscriptionKeyField: JBPasswordField
    private lateinit var regionField: JBTextField

    init {
        title = message("microsoft.settings.dialog.title")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row(message("microsoft.settings.dialog.label.translator")) {
            comboBox(MicrosoftTranslatorType.values().toList())
                .component.apply {
                    translatorComboBox = this
                    selectedItem = selectedTranslator
                    renderer = SimpleListCellRenderer.create { label, value, _ -> label.text = value.displayName }
                    addItemListener { selectedTranslator = selectedItem as MicrosoftTranslatorType }
                }
        }
        row(message("microsoft.settings.dialog.label.subscription.key")) {
            passwordField()
                .component.apply {
                    subscriptionKeyField = this
                }
        }
        row(message("microsoft.settings.dialog.label.region")) {
            textField()
                .component.apply {
                    regionField = this
                    text = selectedRegion
                    document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: javax.swing.event.DocumentEvent) {
                            selectedRegion = text
                        }
                    })
                }
        }
    }

    override fun doOKAction() {
        if (selectedTranslator == MicrosoftTranslatorType.AZURE &&
            subscriptionKeyField.password.isNullOrEmpty() &&
            !settings.isSubscriptionKeySet
        ) {
            setErrorText(message("microsoft.settings.dialog.error.missing.subscription.key"), subscriptionKeyField)
            return
        }

        keyEditor.applyEditing()
        settings.translator = selectedTranslator
        settings.region = selectedRegion.trim()
        super.doOKAction()
    }

    override fun show() {
        super.show()
        keyEditor.startEditing(subscriptionKeyField)
    }
}
