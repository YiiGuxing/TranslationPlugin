package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBEmptyBorder
import javax.swing.JLabel

/**
 * DialogTranslationPanel
 */
class DialogTranslationPane(project: Project?, settings: Settings) :
    TranslationPane<JLabel>(project, settings) {

    private var onBeforeFoldingExpandHandler: (() -> Unit)? = null

    override val originalFoldingLength: Int = 50

    override fun onCreateLanguageComponent() = JLabel().apply {
        border = JBEmptyBorder(0, 0, 0, 10)
    }

    override fun JLabel.updateLanguage(lang: Lang?) {
        text = lang?.langName
    }

    override fun onBeforeFoldingExpand() {
        onBeforeFoldingExpandHandler?.invoke()
    }

    fun onBeforeFoldingExpand(handler: () -> Unit) {
        onBeforeFoldingExpandHandler = handler
    }

}