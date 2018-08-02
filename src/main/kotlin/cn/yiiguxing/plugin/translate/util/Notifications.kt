package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.HTML_DESC_COPY_TO_CLIPBOARD
import cn.yiiguxing.plugin.translate.OptionsConfigurable
import com.intellij.notification.*
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

object Notifications {

    fun showErrorNotification(project: Project?,
                              displayId: String,
                              title: String,
                              message: String,
                              throwable: Throwable) {
        NotificationGroup(displayId, NotificationDisplayType.TOOL_WINDOW, true)
                .createNotification(
                        title,
                        """$message (<a href="$HTML_DESC_COPY_TO_CLIPBOARD">Copy to Clipboard</a>)""",
                        NotificationType.WARNING,
                        object : NotificationListener.Adapter() {
                            override fun hyperlinkActivated(notification: Notification, event: HyperlinkEvent) {
                                notification.expire()
                                when (event.description) {
                                    HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog(project)
                                    HTML_DESC_COPY_TO_CLIPBOARD -> throwable.copyToClipboard()
                                }
                            }
                        })
                .show(project)
    }

}