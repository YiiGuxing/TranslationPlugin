/*
 * Popups
 */

@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Point
import javax.swing.Icon
import javax.swing.JTextField
import javax.swing.text.JTextComponent


open class SpeedSearchListPopupStep<T> : BaseListPopupStep<T> {

    constructor(
        vararg values: T,
        title: String? = null,
        icons: Array<Icon> = emptyArray()
    ) : super(title, values, icons)

    constructor(values: List<T>, title: String? = null, icons: List<Icon> = emptyList()) : super(title, values, icons)

    constructor(values: List<T>, sameIcon: Icon, title: String? = null) : super(title, values, sameIcon)

    final override fun isSpeedSearchEnabled(): Boolean = true

}

inline fun <T> Editor.showListPopup(
    step: ListPopupStep<T>,
    maxRowCount: Int = -1,
    init: (ListPopup) -> Unit = {}
): ListPopup {
    val factory = JBPopupFactory.getInstance()
    val popup = factory.createListPopup(step, maxRowCount)

    val minWidth = if (this is TextComponentEditor) {
        val contentComponent = contentComponent
        if (contentComponent is JTextField) {
            contentComponent.width -
                    with(contentComponent.insets) { left + right } -
                    with(contentComponent.margin) { left + right } +
                    JBUI.scale(2)
        } else JBUI.scale(150)
    } else {
        JBUI.scale(150)
    }
    popup.setMinimumSize(Dimension(minWidth, 0))
    popup.setRequestFocus(true)
    init(popup)

    if (this is TextComponentEditor) {
        popup.show(guessBestPopupLocation)
    } else {
        popup.show(factory.guessBestPopupLocation(this))
    }

    return popup
}


val TextComponentEditor.guessBestPopupLocation: RelativePoint
    get() {
        val component = contentComponent as JTextComponent
        val visibleRect = component.visibleRect
        val popupMenuPoint = when {
            component is JTextField -> {
                val insets = component.insets
                val margin = component.margin
                val x = insets.left + margin.left - JBUI.scale(1)
                val y = visibleRect.height + if (insets.bottom + margin.bottom <= 0) JBUI.scale(2) else 0
                Point(x, y)
            }
            component.hasSelection -> {
                val startRect = component.modelToView(component.selectionStart)
                val endRect = component.modelToView(component.selectionEnd)
                val x = minOf(startRect.x, endRect.x)
                val y = maxOf(startRect.y, endRect.y) + endRect.height
                Point(x, y)
            }
            else -> {
                val caretPosition = component.caret.magicCaretPosition
                val modelRect = component.modelToView(component.caret.dot)
                Point(caretPosition.x, caretPosition.y + modelRect.height)
            }
        }
        popupMenuPoint.translate(visibleRect.x, visibleRect.y)

        return RelativePoint(component, popupMenuPoint)
    }

private val JTextComponent.hasSelection: Boolean get() = selectionStart != selectionEnd