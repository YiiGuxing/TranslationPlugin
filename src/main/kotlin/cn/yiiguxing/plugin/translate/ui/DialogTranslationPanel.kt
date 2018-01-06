package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBEmptyBorder
import javax.swing.JLabel

/**
 * DialogTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/27
 */
class DialogTranslationPanel(project: Project?, settings: Settings) : TranslationPanel<JLabel>(project, settings) {

    override fun onCreateLanguageComponent() = JLabel().apply {
        border = JBEmptyBorder(0, 0, 0, 10)
    }

    override fun JLabel.updateLanguage(lang: Lang?) {
        text = lang?.langName
    }

}