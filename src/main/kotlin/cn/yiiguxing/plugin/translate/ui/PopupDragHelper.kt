package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.Disposer
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class PopupDragHelper private constructor(private var popup: JBPopup) : MouseAdapter() {
    private var initialClick: Point? = null

    override fun mousePressed(e: MouseEvent) {
        initialClick = e.getPoint()
    }

    override fun mouseDragged(e: MouseEvent) {
        val initialClick = initialClick ?: return
        val location = popup.locationOnScreen
        val x = location.x + e.getX() - initialClick.x
        val y = location.y + e.getY() - initialClick.y
        popup.setLocation(Point(x, y))
        e.consume()
    }

    companion object {
        fun dragPopupByComponent(popup: JBPopup, component: JComponent) {
            val listener = PopupDragHelper(popup)
            component.addMouseListener(listener)
            component.addMouseMotionListener(listener)
            Disposer.register(popup) {
                component.removeMouseMotionListener(listener)
                component.removeMouseListener(listener)
            }
        }
    }
}