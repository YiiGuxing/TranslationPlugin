package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.openapi.ui.GraphicsConfig
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.geom.Path2D
import javax.swing.Icon

/**
 * ComboArrowIcon
 */
class ComboArrowIcon(var color: Color = JBColor.WHITE) : Icon {

    private val shape: Shape

    init {
        val w = iconWidth.toDouble()
        val h = iconHeight.toDouble()
        shape = Path2D.Double().apply {
            moveTo(0.0, 0.0)
            lineTo(w, 0.0)
            lineTo(w / 2.0, h)
            closePath()
        }
    }

    override fun getIconWidth(): Int = JBUI.scale(7)

    override fun getIconHeight(): Int = JBUI.scale(5)

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        g as Graphics2D

        val config = GraphicsConfig(g)
        config.setupRoundedBorderAntialiasing()

        with(g) {
            translate(x, y)
            color = this@ComboArrowIcon.color
            g.fill(shape)
            translate(-x, -y)
        }

        config.restore()
    }
}