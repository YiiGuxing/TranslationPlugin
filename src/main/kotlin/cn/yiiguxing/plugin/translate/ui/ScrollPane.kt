package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBEmptyBorder
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

    protected open fun getFadingEdgeColor(): Color? = UIManager.getColor("Panel.background")

    protected open fun getFadingEdgeSize(): Int = 20

    /**
     * Default: [FADING_ALL]
     *
     * @see FADING_START
     * @see FADING_END
     * @see FADING_ALL
     */
    protected open fun getFadingFlag(): Int = FADING_ALL

    final override fun createViewport(): JViewport {
        val fadingFlag = getFadingFlag()
        val fadingEdgeSize = getFadingEdgeSize()
        val fadingEdgeColor = getFadingEdgeColor()
        return if (fadingEdgeColor == null || fadingEdgeSize <= 0 || (fadingFlag and FADING_ALL) == FADING_NONE) {
            super.createViewport()
        } else {
            Viewport(fadingEdgeColor, fadingEdgeSize, fadingFlag)
        }
    }

    @Suppress("unused")
    companion object {
        const val FADING_NONE = 0
        const val FADING_START = 1
        const val FADING_END = 2
        const val FADING_ALL = FADING_START or FADING_END
    }

    class Viewport(
        fadingEdgeColor: Color,
        fadingEdgeSize: Int,
        private val fadingFlag: Int = FADING_ALL
    ) : JBViewport() {

        private val transparent = fadingEdgeColor.withAlpha(0f)
        private val fadingEdgeSize = JBUIScale.scale(fadingEdgeSize)
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
                if (locationY < 0 && (fadingFlag and FADING_START) > 0) {
                    paint = fadingEdgeStart
                    fillRect(0, 0, width, fadingEdgeSize)
                }
                if (view.height + locationY > height && (fadingFlag and FADING_END) > 0) {
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