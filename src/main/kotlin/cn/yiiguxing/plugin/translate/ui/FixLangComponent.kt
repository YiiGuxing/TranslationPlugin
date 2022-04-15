package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import com.intellij.ui.components.panels.HorizontalLayout
import javax.swing.JLabel
import javax.swing.JPanel

class FixLangComponent : JPanel() {
    private val actionLink: ActionLink
    private var onFixLanguageHandler: ((Lang) -> Unit)? = null

    private var lastTranslation: Translation? = null

    init {
        layout = HorizontalLayout(0)
        isOpaque = false
        isVisible = false
        actionLink = ActionLink {
            lastTranslation?.sourceLanguages?.firstOrNull()?.let { lang -> onFixLanguageHandler?.invoke(lang) }
        }
        add(JLabel("${message("tip.label.sourceLanguage")}: "), HorizontalLayout.LEFT)
        add(actionLink, HorizontalLayout.LEFT)
    }

    fun onFixLanguage(handler: (lang: Lang) -> Unit) {
        onFixLanguageHandler = handler
    }

    fun updateOnTranslation(translation: Translation?) {
        lastTranslation = translation
        val sourceLanguages = translation?.sourceLanguages
        if (sourceLanguages != null && !sourceLanguages.contains(translation.srcLang)) {
            isVisible = sourceLanguages.firstOrNull()?.langName.let {
                actionLink.text = it
                !it.isNullOrEmpty()
            }
        } else {
            isVisible = false
        }
    }
}