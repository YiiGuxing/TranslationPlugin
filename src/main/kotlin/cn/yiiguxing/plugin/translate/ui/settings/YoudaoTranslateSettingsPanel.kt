package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.YoudaoTranslateSettings
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.LanguageRenderer
import cn.yiiguxing.plugin.translate.ui.form.YoudaoTranslateSettingsForm
import cn.yiiguxing.plugin.translate.ui.selected
import com.intellij.ui.CollectionComboBoxModel
import javax.swing.Icon
import javax.swing.JComponent

/**
 * YoudaoTranslateSettingsPanel
 *
 * Created by Yii.Guxing on 2018/1/18
 */
class YoudaoTranslateSettingsPanel(
        private val settings: YoudaoTranslateSettings
) : YoudaoTranslateSettingsForm(), TranslatorSettingsPanel {

    override val id: String = YoudaoTranslator.id
    override val name: String = "有道翻译"
    override val icon: Icon = YoudaoTranslator.icon
    override val component: JComponent = contentPanel

    init {
        primaryLanguage.apply {
            model = CollectionComboBoxModel(YoudaoTranslator.SUPPORTED_LANGUAGES)
            setRenderer(LanguageRenderer)
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
            if (YoudaoTranslator.SUPPORTED_LANGUAGES.contains(it)) {
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