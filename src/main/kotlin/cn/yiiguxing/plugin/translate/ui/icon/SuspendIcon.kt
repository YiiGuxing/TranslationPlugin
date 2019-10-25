package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * SuspendIcon
 */
class SuspendIcon(val color: Color) : Icon {

    override fun getIconWidth(): Int = JBUI.scale(14)
    override fun getIconHeight(): Int = JBUI.scale(14)

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        val padding = JBUI.scale(2)

        with(g) {
            val oldColor = color
            color = this@SuspendIcon.color
            fillRect(x + padding, y + padding, iconWidth - padding * 2, iconHeight - padding * 2)
            color = oldColor
        }
    }
}