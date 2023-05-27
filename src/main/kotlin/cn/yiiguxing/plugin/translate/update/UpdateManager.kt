package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.impl.NotificationsManagerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.BalloonImpl
import com.intellij.ui.BalloonLayoutData
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import java.awt.Color
import java.awt.Point
import java.util.*
import javax.swing.UIManager

class UpdateManager : BaseStartupActivity() {

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
        val color = getBorderColor()
        val partStyle = "margin-top: ${JBUI.scale(8)}px;"
        val milestone = if (!version.isRreRelease) {
            val refStyle =
                "padding: ${JBUI.scale(3)}px ${JBUI.scale(6)}px; border-left: ${JBUI.scale(3)}px solid $color;"
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

        val canBrowseWhatsNewHTMLEditor = canBrowseWhatsNewHTMLEditor()
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(UPDATE_NOTIFICATION_GROUP_ID) ?: return false
        val notification = notificationGroup
            .createNotification(content, NotificationType.INFORMATION)
            .setTitle(title)
            .setImportant(true)
            .apply {
                setListener(Notifications.UrlOpeningListener(false))
            }
            .apply {
                if (!version.isRreRelease && isFeatureVersion && !canBrowseWhatsNewHTMLEditor) {
                    addAction(WhatsNewAction(version))
                }
            }
            .addAction(GetStartedAction())
            .addAction(SupportAction())
            .whenExpired {
                if (!version.isRreRelease && isFeatureVersion && canBrowseWhatsNewHTMLEditor) {
                    browseWhatsNew(version, project)
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

    private class WhatsNewAction(private val version: Version) :
        DumbAwareAction(
            message("action.WhatsNewInTranslationAction.text", version.getFeatureUpdateVersion()),
            null,
            AllIcons.General.Web
        ) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(version.getWhatsNewUrl(true))
    }

    private class GetStartedAction :
        DumbAwareAction(message("action.GetStartedAction.text"), null, AllIcons.General.Web) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(WebPages.getStarted())
    }


    companion object {
        const val UPDATE_NOTIFICATION_GROUP_ID = "Translation Plugin Updates"

        private const val VERSION_PROPERTY = "${TranslationPlugin.PLUGIN_ID}.version"

        private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=milestone%%3Av%s+is%%3Aclosed"


        private val LOG = Logger.getInstance(UpdateManager::class.java)

        private val defaultTitleBarHeight: Int
            get() = JBUIScale.scale(30)

        private fun getBorderColor(): String {
            val color = UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR
            return color.toRGBHex()
        }

        fun Version.getWhatsNewUrl(frame: Boolean = false, locale: Locale = Locale.getDefault()): String {
            val v = getFeatureUpdateVersion()
            return if (frame) {
                WebPages.updates(v, locale = locale)
            } else {
                val isDark = UIUtil.isUnderDarcula()
                WebPages.releaseNote(v, isDark, locale = locale)
            }
        }

        fun canBrowseWhatsNewHTMLEditor(): Boolean {
            return JBCefApp.isSupported()
        }

        fun browseWhatsNew(version: Version, project: Project?) {
            if (project != null && canBrowseWhatsNewHTMLEditor()) {
                invokeLater(ModalityState.NON_MODAL, expired = project.disposed) {
                    try {
                        HTMLEditorProvider.openEditor(
                            project,
                            adaptedMessage("action.WhatsNewInTranslationAction.text", "Translation"),
                            version.getWhatsNewUrl(),
                            //language=HTML
                            """<div style="text-align: center;padding-top: 3rem">
                            |<div style="padding-top: 1rem; margin-bottom: 0.8rem;">Failed to load!</div>
                            |<div><a href="${version.getWhatsNewUrl(true)}" target="_blank"
                            |        style="font-size: 2rem">Open in browser</a></div>
                            |</div>""".trimMargin()
                        )
                    } catch (e: Throwable) {
                        LOG.w("""Failed to load "What's New" page""", e)
                        BrowserUtil.browse(version.getWhatsNewUrl(true))
                    }
                }
            } else {
                BrowserUtil.browse(version.getWhatsNewUrl(true))
            }
        }
    }
}