/*
 * Popups
 */

@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.Balloon.Position.*
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.ScreenUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.geom.Rectangle2D
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.text.JTextComponent


/**
 * Popups
 */
object Popups {

    private val LOG = Logger.getInstance(Popups::class.java)

    fun showBalloonForActiveFrame(message: String, type: MessageType) {
        val frame = IdeFocusManager.findInstance().lastFocusedFrame
        if (frame == null) {
            val projects = ProjectManager.getInstance().openProjects
            val project = if (projects.isEmpty()) ProjectManager.getInstance().defaultProject else projects[0]
            val jFrame = WindowManager.getInstance().getFrame(project)
            if (jFrame != null) {
                showBalloonForComponent(jFrame, message, type, project)
            } else {
                LOG.info("Can not get component to show message: $message")
            }
            return
        }

        showBalloonForComponent(frame.component, message, type, frame.project, offsetY = 10)
    }

    fun showBalloonForComponent(
        component: Component,
        message: String,
        type: MessageType,
        project: Project?,
        position: Balloon.Position = below,
        offsetX: Int = 0,
        offsetY: Int = 0
    ) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setDisposable(TranslationUIManager.disposable(project))
            .createBalloon()
        val point = component.size?.let { size ->
            var x = 0
            var y = 0
            if (position == atRight) {
                x = size.width
            }
            if (position == below) {
                y = size.height
            }
            if (position == below || position == above) {
                x = size.width / 2
            }
            if (position == atLeft || position == atRight) {
                y = size.height / 2
            }

            Point(x + offsetX, y + offsetY)
        } ?: Point(offsetX, offsetY)
        balloon.show(RelativePoint(component, point), position)
    }
}


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
                val emptyRect = Rectangle2D.Float()
                val startRect = component.modelToView2D(component.selectionStart) ?: emptyRect
                val endRect = component.modelToView2D(component.selectionEnd) ?: emptyRect
                val x = minOf(startRect.x, endRect.x)
                val y = maxOf(startRect.y, endRect.y) + endRect.height
                Point(x.toInt(), y.toInt())
            }

            else -> {
                val caretPosition = component.caret.magicCaretPosition ?: Point()
                val modelRect = component.modelToView2D(component.caret.dot) ?: Rectangle2D.Float()
                Point(caretPosition.x, caretPosition.y + modelRect.height.toInt())
            }
        }
        popupMenuPoint.translate(visibleRect.x, visibleRect.y)

        return RelativePoint(component, popupMenuPoint)
    }

private val JTextComponent.hasSelection: Boolean get() = selectionStart != selectionEnd

fun JBPopup.showBelow(component: JComponent, offsetX: Int = 0, offsetY: Int = 0) {
    val belowLocation = RelativePoint(
        component,
        Point(JBUI.scale(offsetX), component.height + JBUI.scale(offsetY))
    )

    addListener(object : JBPopupListener {
        override fun beforeShown(event: LightweightWindowEvent) {
            val popup = event.asPopup()
            val screen = ScreenUtil.getScreenRectangle(component.locationOnScreen)
            val above = screen.height < popup.size.height + belowLocation.screenPoint.y

            if (above) {
                val aboveLocation = RelativePoint(component, Point(JBUI.scale(offsetX), -JBUI.scale(offsetY)))
                val point = Point(aboveLocation.screenPoint)
                point.translate(0, -popup.size.height)
                popup.setLocation(point)
            }
        }
    })
    show(belowLocation)
}