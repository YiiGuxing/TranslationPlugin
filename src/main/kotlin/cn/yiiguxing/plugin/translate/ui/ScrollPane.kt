package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JViewport
import javax.swing.UIManager

/**
 * ScrollPane
 */
open class ScrollPane(view: Component) : JBScrollPane(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER) {

    init {
        isOpaque = false
        border = JBEmptyBorder(0)
    }

    /**
     * @throws RuntimeException
     */
    override fun setHorizontalScrollBarPolicy(policy: Int) {
        if (policy != HORIZONTAL_SCROLLBAR_NEVER) {
            throw RuntimeException("Can not change horizontal scroll bar policy.")
        }

        super.setHorizontalScrollBarPolicy(policy)
    }

    override fun createViewport(): JViewport {
        return Viewport(UIManager.getColor("Panel.background"), 20)
    }

    class Viewport(fadingEdgeColor: Color, fadingEdgeSize: Int) : JBViewport() {

        private val transparent = fadingEdgeColor.withAlpha(0f)
        private val fadingEdgeSize = JBUI.scale(fadingEdgeSize)
        private val fadingEdgeStart = GradientPaint(
            0f, 0f, fadingEdgeColor,
            0f, this.fadingEdgeSize.toFloat(), transparent
        )
        private val fadingEdgeEnd = GradientPaint(
            0f, 0f, transparent,
            0f, this.fadingEdgeSize.toFloat(), fadingEdgeColor
        )

        init {
            // 不绘制背景，包括图像背景
            isOpaque = false
        }

        override fun paint(g: Graphics) {
            super.paint(g)

            val view = view ?: return
            with(g as Graphics2D) {
                val oldPaint = paint
                val locationY = view.location.y
                if (locationY < 0) {
                    paint = fadingEdgeStart
                    fillRect(0, 0, width, fadingEdgeSize)
                }
                if (view.height + locationY > height) {
                    val translateY = height - fadingEdgeSize
                    paint = fadingEdgeEnd
                    translate(0, translateY)
                    fillRect(0, 0, width, fadingEdgeSize)
                    translate(0, -translateY)
                }
                paint = oldPaint
            }
        }
    }
}