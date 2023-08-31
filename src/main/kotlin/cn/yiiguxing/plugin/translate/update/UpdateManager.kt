package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.Hyperlinks
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.show
import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.impl.NotificationsManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonLayoutData
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import java.awt.Color
import java.awt.Point
import javax.swing.UIManager


private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

private val defaultTitleBarHeight: Int
    get() = JBUIScale.scale(if (Registry.`is`("ide.experimental.ui", true)) 40 else 30)

private val borderColor: String
    get() = (UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR).toRGBHex()


class UpdateManager : BaseStartupActivity() {

    companion object {
        internal const val UPDATE_NOTIFICATION_GROUP_ID = "Translation Plugin updated"

        private const val VERSION_PROPERTY = "${TranslationPlugin.PLUGIN_ID}.version"

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=milestone%%3Av%s+is%%3Aclosed"

        private val LOG = logger<UpdateManager>()
    }

    override fun onRunActivity(project: Project) {
        checkUpdate(project)
    }

    private fun checkUpdate(project: Project) {
        val plugin = TranslationPlugin.descriptor
        val versionString = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val lastVersionString = properties.getValue(VERSION_PROPERTY, Version.INITIAL_VERSION)
        if (versionString == lastVersionString) {
            return
        }

        val version = Version(versionString)
        val lastVersion = Version.getOrElse(lastVersionString) { Version() }
        if (version.isSameVersion(lastVersion)) {
            return
        }

        val isFeatureVersion = version.isFeatureUpdateOf(lastVersion)
        if (showUpdateNotification(project, plugin, version, isFeatureVersion)) {
            properties.setValue(VERSION_PROPERTY, versionString)
        }
    }

    private fun showUpdateNotification(
        project: Project,
        plugin: IdeaPluginDescriptor,
        version: Version,
        isFeatureVersion: Boolean
    ): Boolean {
        val title = message(
            "plugin.name.updated.to.version.notification.title",
            plugin.name,
            version.getVersionWithoutBuildMetadata()
        )
        val partStyle = "margin-top: ${JBUI.scale(8)}px;"
        val milestone = if (!version.isRreRelease) {
            val refStyle =
                "padding: ${JBUI.scale(3)}px ${JBUI.scale(6)}px; border-left: ${JBUI.scale(3)}px solid $borderColor;"
            message(
                "plugin.updated.notification.message.milestone",
                "$partStyle $refStyle",
                MILESTONE_URL.format(version.getStableVersion())
            )
        } else ""
        val content = message(
            "plugin.updated.notification.message",
            Hyperlinks.SUPPORT_DESCRIPTION,
            milestone,
            partStyle,
            plugin.changeNotes ?: "<ul><li></li></ul>"
        )

        val canBrowseWhatsNewInHTMLEditor = WhatsNew.canBrowseInHTMLEditor
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID) ?: return false
        val notification = notificationGroup
            .createNotification(content, NotificationType.INFORMATION)
            .setTitle(title)
            .setImportant(true)
            .setIcon(TranslationIcons.Logo)
            .apply {
                setListener(Notifications.UrlOpeningListener(false))
            }
            .apply {
                if (!version.isRreRelease && isFeatureVersion && !canBrowseWhatsNewInHTMLEditor) {
                    addAction(WhatsNew.Action(version))
                }
            }
            .addAction(GetStartedAction())
            .addAction(SupportAction())
            .whenExpired {
                if (!version.isRreRelease && isFeatureVersion && canBrowseWhatsNewInHTMLEditor) {
                    WhatsNew.browse(version, project)
                }
            }

        if (!notification.notifyByBalloon(project)) {
            notification.show(project)
        }

        return true
    }

    private fun Notification.notifyByBalloon(project: Project): Boolean {
        return try {
            val window = NotificationsManagerImpl.findWindowForBalloon(project) as? IdeFrame ?: return false
            val uiDisposable = TranslationUIManager.disposable(project)
            val balloon = NotificationsManagerImpl.createBalloon(
                window,
                this,
                true,
                false,
                BalloonLayoutData.fullContent(),
                uiDisposable
            )

            if (balloon.isDisposed) {
                return false
            }

            balloon as BalloonImpl
            balloon.setShowPointer(false)
            Disposer.register(balloon) { expire() }

            balloon.showInIdeFrame(window)
        } catch (e: Throwable) {
            LOG.error(e)
            false
        }
    }

    private fun BalloonImpl.showInIdeFrame(window: IdeFrame): Boolean {
        val component = window.component ?: return false
        val target = if (SystemInfoRt.isMac) {
            RelativePoint(component, Point(component.width, 0))
        } else {
            val layeredPane = component.rootPane?.layeredPane
            val titleBar = layeredPane?.components?.find {
                it.x == 0 && it.y == 0 && it.width == layeredPane.width && it.height > 0
            }

            val insetTop = shadowBorderInsets.top
            val contentHalfHeight = content.preferredSize.height / 2
            val titleBarHeight = titleBar?.height ?: defaultTitleBarHeight
            val offsetY = titleBarHeight + insetTop + contentHalfHeight
            val relativeComponent = titleBar ?: component
            RelativePoint(relativeComponent, Point(relativeComponent.width, offsetY))
        }

        show(target, Balloon.Position.atLeft)
        return true
    }

    private class SupportAction : DumbAwareAction(message("support.notification"), null, TranslationIcons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    private class GetStartedAction :
        DumbAwareAction(message("action.GetStartedAction.text"), null, AllIcons.General.Web) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(WebPages.docs())
    }
}