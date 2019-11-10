package cn.yiiguxing.plugin.translate.ui

import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.UIUtil
import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/**
 * List item circle component
 */
class ListItemCircleComponent : JComponent() {

    init {
        preferredSize = JBDimension(9, 9)
        foreground = UIUtil.getLabelForeground()
    }

    override fun getComponentGraphics(g: Graphics): Graphics {
        return (super.getComponentGraphics(g) as Graphics2D).apply {
            composite = ALPHA_COMPOSITE
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }

    override fun paintComponent(g: Graphics) {
        g.fillOval(0, 0, width, height)
    }

    companion object {
        private val ALPHA_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, .7f)
    }

}