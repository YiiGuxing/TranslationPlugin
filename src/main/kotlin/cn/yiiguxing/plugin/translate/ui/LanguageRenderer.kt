package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.ui.ListCellRendererWrapper
import javax.swing.JList

/**
 * LanguageRenderer
 *
 * Created by Yii.Guxing on 2017-11-08 0008.
 */
object LanguageRenderer : ListCellRendererWrapper<Lang>() {
    override fun customize(list: JList<*>?, value: Lang, index: Int, selected: Boolean, hasFocus: Boolean) {
        setText(value.langName)
    }
}