package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.Consumer
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Icon

/**
 * Icon Button.
 */
open class IconButton(
        private val icon: Icon,
        private val pressIcon: Icon?,
        private val listener: Consumer<MouseEvent>
) : NonOpaquePanel() {
    private var isPressedByMouse: Boolean = false
    private var isActive = true

    init {
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                this@IconButton.listener.consume(e)
            }

            override fun mousePressed(e: MouseEvent) {
                isPressedByMouse = true
                this@IconButton.repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                isPressedByMouse = false
                this@IconButton.repaint()
            }

            override fun mouseEntered(e: MouseEvent) {}

            override fun mouseExited(e: MouseEvent) {
                isPressedByMouse = false
                this@IconButton.repaint()
            }
        })
    }

    override final fun addMouseListener(l: MouseListener?) {
        super.addMouseListener(l)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(icon.iconWidth, icon.iconHeight)
    }

    fun setActive(active: Boolean) {
        this.isActive = active
        this.repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (hasPaint()) {
            paintIcon(g, if ((!isActive || isPressedByMouse) && pressIcon != null) pressIcon else icon)
        }
    }

    protected open fun hasPaint() = width > 0

    protected open fun paintIcon(g: Graphics, icon: Icon) {
        icon.paintIcon(this, g, 0, (height - icon.iconHeight) / 2)
    }

}
