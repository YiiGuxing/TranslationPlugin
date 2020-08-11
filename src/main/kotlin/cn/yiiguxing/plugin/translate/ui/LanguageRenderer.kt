package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList

/**
 * LanguageRenderer
 */
class LanguageRenderer : SimpleListCellRenderer<Lang>() {
    override fun customize(list: JList<out Lang>, value: Lang, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = value.langName
    }
}