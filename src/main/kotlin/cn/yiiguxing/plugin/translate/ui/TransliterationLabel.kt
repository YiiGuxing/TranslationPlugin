package cn.yiiguxing.plugin.translate.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil.toHtml
import java.awt.datatransfer.StringSelection
import javax.swing.JLabel

class TransliterationLabel(private val maxLength: Int) : JLabel() {
    init {
        val copy = JBMenuItem("Copy", AllIcons.Actions.Copy)
        copy.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
        componentPopupMenu = JBPopupMenu().apply { add(copy) }
    }

    override fun setText(text: String?) {
        if (text == null) {
            isVisible = false
        } else {
            super.setText(wrapHtmlBody(StringUtil.first(text, maxLength, /*appendEllipsis*/ true)))
            isVisible = true
            toolTipText = wrapHtmlBody("""<p width="400">$text</p>""")
        }
    }

    private fun wrapHtmlBody(text: String) = """<html><body>$text</body></html>"""
}