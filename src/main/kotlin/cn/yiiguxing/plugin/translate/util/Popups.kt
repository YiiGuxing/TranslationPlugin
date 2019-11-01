@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Point

/**
 * Popups
 */
object Popups {

    private val LOG = Logger.getInstance(Popups::class.java)

    @Suppress("unused")
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
        offsetX: Int = 0,
        offsetY: Int = 0
    ) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setDisposable(project ?: ApplicationManager.getApplication())
            .createBalloon()
        val (point, position) = component.size?.let { size ->
            Point(size.width / 2 + offsetX, size.height + offsetY) to Balloon.Position.below
        } ?: Point(offsetX, offsetY) to Balloon.Position.above
        balloon.show(RelativePoint(component, point), position)
    }
}