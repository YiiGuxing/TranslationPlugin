package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import java.awt.event.ItemEvent
import java.awt.event.ItemListener

/**
 * BalloonTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/13
 */
class BalloonTranslationPanel(settings: Settings, maxWidth: Int)
    : TranslationPanel<ComboBox<Lang>>(settings, maxWidth) {

    private var ignoreEvent = false
    private var onLanguageChangedHandler: ((Lang, Lang) -> Unit)? = null
    private val itemListener = ItemListener {
        with(it) {
            if (!ignoreEvent && stateChange == ItemEvent.SELECTED) {
                val src = sourceLangComponent.selected
                val target = targetLangComponent.selected
                if (src != null && target != null) {
                    onLanguageChangedHandler?.invoke(src, target)
                }
            }
        }
    }

    override fun onCreateLanguageComponent(): ComboBox<Lang> = ComboBox<Lang>().apply {
        ui = LangComboBoxUI(this)
        addItemListener(itemListener)
    }

    override fun ComboBox<Lang>.updateLanguage(lang: Lang?) {
        ignoreEvent = true
        selected = lang
        ignoreEvent = false
    }

    fun setSupportedLanguages(src: List<Lang>, target: List<Lang>) {
        sourceLangComponent.setLanguages(src)
        targetLangComponent.setLanguages(target)
    }

    fun onLanguageChanged(handler: (src: Lang, target: Lang) -> Unit) {
        onLanguageChangedHandler = handler
    }

    companion object {
        private fun ComboBox<Lang>.setLanguages(languages: List<Lang>) {
            model = CollectionComboBoxModel<Lang>(languages, selected)
        }
    }

}