package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppKeySettings
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.ui.LanguageRenderer
import cn.yiiguxing.plugin.translate.ui.form.AppKeySettingsForm
import cn.yiiguxing.plugin.translate.ui.selected
import com.intellij.ui.CollectionComboBoxModel
import javax.swing.Icon
import javax.swing.JComponent

/**
 * AppKeySettingsPanel
 */
abstract class AppKeySettingsPanel(private val settings: AppKeySettings, translator: Translator)
    : AppKeySettingsForm(), TranslatorSettingsPanel {

    override val component: JComponent = contentPanel

    override val id: String = translator.id
    override val icon: Icon = translator.icon

    private val supportedLanguages = translator.supportedTargetLanguages

    init {
        primaryLanguage.apply {
            model = CollectionComboBoxModel(supportedLanguages)
            setRenderer(LanguageRenderer())
        }
    }

    private var appKey: String?
        get() = appKeyField.password
                ?.takeIf { it.isNotEmpty() }
                ?.let { String(it) }
                ?: ""
        set(value) {
            appKeyField.text = if (value.isNullOrEmpty()) null else value
        }

    override val isModified: Boolean
        get() {
            val settings = settings
            return appIdField.text != settings.appId ||
                    appKey != settings.getAppKey() ||
                    primaryLanguage.selectedItem != settings.primaryLanguage
        }

    override fun reset() {
        val settings = settings
        appIdField.text = settings.appId
        appKey = settings.getAppKey()

        settings.primaryLanguage.let {
            if (supportedLanguages.contains(it)) {
                primaryLanguage.selectedItem = it
            }
        }
    }

    override fun apply() {
        val settings = settings
        settings.appId = appIdField.text
        settings.setAppKey(appKey)

        primaryLanguage.selected?.let {
            settings.primaryLanguage = it
        }
    }
}