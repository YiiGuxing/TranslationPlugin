package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Presenter
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList


class HistoryRenderer(
    private val sourceLangProvider: () -> Lang?,
    private val targetLangProvider: () -> Lang?,
    private val presenter: Presenter
) : SimpleListCellRenderer<String>() {

    private val builder = StringBuilder()

    override fun customize(
        list: JList<out String>,
        value: String?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (list.width == 0 || value.isNullOrBlank()) { // 在没有确定大小之前不设置真正的文本,否则控件会被过长的文本撑大.
            text = null
        } else {
            setRenderText(value)
        }
    }


    private fun setRenderText(value: String) {
        val text = with(builder) {
            setLength(0)


            append("<html><body><b>")
            append(trim(value))
            append("</b>")

            val src = sourceLangProvider()
            val target = targetLangProvider()
            if (src != null && target != null) {
                presenter.getCache(value, src, target)?.let {
                    append("  -  <i><small>")
                    append(trim(it.translation))
                    append("</small></i>")
                }
            }

            builder.append("</body></html>")
            toString()
        }
        setText(text)
    }

    private fun trim(value: String?): String? {
        value ?: return null

        val withoutNewLines = StringUtil.convertLineSeparators(value, "")
        return StringUtil.first(withoutNewLines, 100, /*appendEllipsis*/ true)
    }
}
