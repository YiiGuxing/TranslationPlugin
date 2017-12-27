package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.util.ui.JBEmptyBorder
import javax.swing.JLabel

/**
 * DialogTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/27
 */
class DialogTranslationPanel(settings: Settings) : TranslationPanel<JLabel>(settings) {

    override fun onCreateLanguageComponent() = JLabel().apply {
        border = JBEmptyBorder(0, 0, 0, 10)
    }

    override fun JLabel.updateLanguage(lang: Lang?) {
        text = lang?.langName
    }

}