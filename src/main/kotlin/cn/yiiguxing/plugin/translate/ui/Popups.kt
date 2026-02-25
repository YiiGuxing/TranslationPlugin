/*
 * Popups
 */

@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.Balloon.Position.*
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.ScreenUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Alarm
import java.awt.Component
import java.awt.Point
import java.awt.geom.Rectangle2D
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.text.JTextComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * Popups
 */
object Popups {


    private val alarm = Alarm()

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
    ): Balloon = showBalloonForComponent(
        component = component,
        message = message,
        type = type,
        position = position,
        anchor = TranslationUIManager.disposable(project),
        offsetX = offsetX,
        offsetY = offsetY
    )

    fun showBalloonForComponent(
        component: Component,
        message: String,
        type: MessageType,
        position: Balloon.Position,
        anchor: Disposable,
        icon: Icon? = type.defaultIcon,
        offsetX: Int = 0,
        offsetY: Int = 0,
        autoHideDelay: Duration = 0.milliseconds
    ): Balloon {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, icon, type.titleForeground, type.popupBackground, null)
            .setBorderColor(type.borderColor)
            .setDisposable(anchor)
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

        val delayMillis = autoHideDelay.inWholeMilliseconds
        if (delayMillis > 0) {
            val hideRequest = { balloon.hide() }
            Disposer.register(balloon) { alarm.cancelRequest(hideRequest) }
            alarm.addRequest(hideRequest, delayMillis, ModalityState.stateForComponent(component))
        }

        return balloon
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
    popup.setRequestFocus(true)
    init(popup)

    popup.showInBestPositionFixed(this)

    return popup
}


fun TextComponentEditor.getGuessBestPopupLocation(): RelativePoint {
    val component = contentComponent as JTextComponent
    val visibleRect = component.visibleRect
    val popupMenuPoint = when {
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
        Point(offsetX.scaled, component.height + offsetY.scaled)
    )

    addListener(object : JBPopupListener {
        override fun beforeShown(event: LightweightWindowEvent) {
            val popup = event.asPopup()
            val screen = ScreenUtil.getScreenRectangle(component.locationOnScreen)
            val above = screen.height < popup.size.height + belowLocation.screenPoint.y

            if (above) {
                val aboveLocation = RelativePoint(component, Point(offsetX.scaled, -(offsetY.scaled)))
                val point = Point(aboveLocation.screenPoint)
                point.translate(0, -popup.size.height)
                popup.setLocation(point)
            }
        }
    })
    show(belowLocation)
}

fun JBPopup.showInBestPositionFixed(editor: Editor) {
    when (editor) {
        is TextComponentEditor -> show(editor.getGuessBestPopupLocation())
        else -> showInBestPositionFor(editor)
    }
}
