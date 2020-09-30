package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.scale.JBUIScale
import java.awt.datatransfer.StringSelection
import javax.swing.JLabel
import kotlin.math.max
import kotlin.math.min

class TransliterationLabel : JLabel() {

    init {
        val copy = JBMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy)
        copy.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
        componentPopupMenu = JBPopupMenu().apply { add(copy) }
    }

    override fun getToolTipText(): String? {
        if (text.isNullOrEmpty()) return null
        val textWidth = getFontMetrics(font).getStringBounds(text, graphics).width.toInt()
        return """<html><body width="${max(10, min(textWidth, JBUIScale.scale(300)))}">$text</body></html>"""
    }

    override fun setText(text: String?) {
        super.setText(text)
        isVisible = !text.isNullOrEmpty()
    }
}