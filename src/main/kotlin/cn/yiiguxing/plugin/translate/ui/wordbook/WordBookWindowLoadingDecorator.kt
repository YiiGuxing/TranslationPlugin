package cn.yiiguxing.plugin.translate.ui.wordbook

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

internal class WordBookWindowLoadingDecorator(content: JComponent, parent: Disposable) :
    LoadingDecorator(content, parent, -1) {

    override fun customizeLoadingLayer(parent: JPanel, text: JLabel, icon: AsyncProcessIcon): NonOpaquePanel {
        parent.layout = GridBagLayout()

        text.font = text.font.let { it.deriveFont(it.size + 6f) }
        text.foreground = ColorUtil.toAlpha(UIUtil.getLabelForeground(), 150)
        text.border = JBUI.Borders.emptyLeft(12)

        val gap = JLabel().iconTextGap * 3
        val result = NonOpaquePanel(VerticalLayout(gap, SwingConstants.CENTER))
        result.add(icon, VerticalLayout.CENTER)
        result.add(text, VerticalLayout.CENTER)
        parent.add(result)

        return result
    }

}