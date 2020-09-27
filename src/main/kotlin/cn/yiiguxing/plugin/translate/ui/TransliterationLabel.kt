package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import java.awt.datatransfer.StringSelection
import javax.swing.JLabel

class TransliterationLabel(private val maxLength: Int) : JLabel() {
    init {
        val copy = JBMenuItem(message("menu.item.copy"), AllIcons.Actions.Copy)
        copy.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
        componentPopupMenu = JBPopupMenu().apply { add(copy) }
    }

    override fun setText(text: String?) {
        if (text == null) {
            isVisible = false
        } else {
            super.setText(text)
            isVisible = true
            toolTipText = text
        }
    }
}