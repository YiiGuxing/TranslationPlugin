package cn.yiiguxing.plugin.translate.ui

import java.awt.Color
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.text.Element
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants as DefaultStyleConstants

/**
 * StyledViewer
 *
 * Created by Yii.Guxing on 2019/10/11.
 */
class StyledViewer : Viewer() {

    private var onClickHandler: ((Element, Any?) -> Unit)? = null
    private var onBeforeFoldingExpandHandler: ((Element, Any?) -> Unit)? = null
    private var onFoldingExpandedHandler: ((Element, Any?) -> Unit)? = null

    init {
        ViewerMouseAdapter().let {
            addMouseListener(it)
            addMouseMotionListener(it)
        }
    }

    fun onClick(handler: ((element: Element, data: Any?) -> Unit)?) {
        onClickHandler = handler
    }

    fun onBeforeFoldingExpand(handler: ((element: Element, data: Any?) -> Unit)?) {
        onBeforeFoldingExpandHandler = handler
    }

    fun onFoldingExpanded(handler: ((element: Element, data: Any?) -> Unit)?) {
        onFoldingExpandedHandler = handler
    }

    private enum class StyleConstants {
        MouseListener;

        companion object {
            fun MutableAttributeSet.setMouseListener(listener: StyledViewer.MouseListener) {
                addAttribute(MouseListener, listener)
            }
        }
    }

    private inner class ViewerMouseAdapter : MouseAdapter() {

        private var lastElement: Element? = null
        private var activeElement: Element? = null

        private inline val MouseEvent.characterElement: Element
            get() = styledDocument.getCharacterElement(viewToModel(point))

        private inline val Element.mouseListener: MouseListener?
            get() = attributes.getAttribute(StyleConstants.MouseListener) as? MouseListener

        override fun mouseMoved(event: MouseEvent) {
            val element = event.characterElement
            if (element !== lastElement) {
                exitLastElement()

                lastElement = element.mouseListener?.run {
                    mouseEntered(this@StyledViewer, element)
                    element
                }
            }
        }

        private fun exitLastElement() {
            activeElement = null
            val element = lastElement ?: return

            element.mouseListener?.mouseExited(this@StyledViewer, element)
            lastElement = null
        }

        override fun mouseExited(event: MouseEvent) = exitLastElement()

        override fun mousePressed(event: MouseEvent) {
            activeElement = event.characterElement
        }

        override fun mouseReleased(event: MouseEvent) { // 使用`mouseClicked`在MacOS下会出现事件丢失的情况...
            with(event) {
                characterElement.takeIf { it === activeElement }?.let { elem ->
                    (elem.attributes.getAttribute(StyleConstants.MouseListener) as? MouseListener)?.run {
                        if (modifiers and MouseEvent.BUTTON1_MASK != 0) {
                            mouseClicked(this@StyledViewer, elem)
                        }

                        if (isMetaDown) {
                            mouseRightButtonClicked(this@StyledViewer, elem)
                        }
                    }
                }
            }
            activeElement = null
        }
    }

    open class MouseListener internal constructor(val data: Any? = null) {

        open fun mouseClicked(viewer: StyledViewer, element: Element) {
            viewer.onClickHandler?.invoke(element, data)
        }

        open fun mouseRightButtonClicked(viewer: StyledViewer, element: Element) {

        }

        open fun mouseEntered(viewer: StyledViewer, element: Element) {
            viewer.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        open fun mouseExited(viewer: StyledViewer, element: Element) {
            viewer.cursor = Cursor.getDefaultCursor()
        }
    }

    open class ColoredMouseListener internal constructor(
        color: Color,
        hoverColor: Color? = null,
        data: Any? = null
    ) : MouseListener(data) {

        private val regularAttributes = SimpleAttributeSet().apply {
            DefaultStyleConstants.setForeground(this, color)
        }
        private val hoverAttributes = SimpleAttributeSet().apply {
            DefaultStyleConstants.setForeground(this, hoverColor ?: color)
        }

        override fun mouseEntered(viewer: StyledViewer, element: Element) {
            super.mouseEntered(viewer, element)

            with(element) {
                viewer.styledDocument.setCharacterAttributes(startOffset, offsetLength, hoverAttributes, false)
            }
        }

        override fun mouseExited(viewer: StyledViewer, element: Element) {
            super.mouseExited(viewer, element)

            with(element) {
                viewer.styledDocument.setCharacterAttributes(startOffset, offsetLength, regularAttributes, false)
            }
        }

    }

    class FoldingMouseListener(
        data: Any? = null,
        private val onFoldingExpand: (viewer: StyledViewer, element: Element, data: Any?) -> Unit
    ) : MouseListener(data) {
        override fun mouseClicked(viewer: StyledViewer, element: Element) {
            viewer.onBeforeFoldingExpandHandler?.invoke(element, data)
            onFoldingExpand(viewer, element, data)
            viewer.onFoldingExpandedHandler?.invoke(element, data)
        }

        override fun mouseRightButtonClicked(viewer: StyledViewer, element: Element) = Unit
    }

    companion object {
        private inline val Element.offsetLength: Int
            get() = endOffset - startOffset
    }

}