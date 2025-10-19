package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Presenter
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList


class HistoryRenderer(
    private val presenter: Presenter,
    private val sourceLangProvider: () -> Lang,
    private val targetLangProvider: () -> Lang
) : ColoredListCellRenderer<String>() {

    override fun customizeCellRenderer(
        list: JList<out String>,
        value: String,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        // 在没有确定大小之前不设置真正的文本,否则控件会被过长的文本撑大
        if (list.width == 0 || value.isBlank()) {
            clear()
            return
        }

        append(value.trimAndEllipsis(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presenter.getCache(value, sourceLangProvider(), targetLangProvider())
            ?.translation?.let {
                append("  →  ${it.trimAndEllipsis()}", SimpleTextAttributes.GRAY_ATTRIBUTES, false)
            }
    }

    private fun String.trimAndEllipsis(): String {
        val withoutNewLines = StringUtil.convertLineSeparators(this, "↩")
        return StringUtil.first(withoutNewLines, 30, true)
    }
}
