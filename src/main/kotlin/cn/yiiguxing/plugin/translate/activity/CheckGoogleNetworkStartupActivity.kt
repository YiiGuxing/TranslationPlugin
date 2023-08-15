package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.trans.google.GoogleSettings
import cn.yiiguxing.plugin.translate.trans.google.TKK
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.runAsync
import java.util.*

class CheckGoogleNetworkStartupActivity : BaseStartupActivity() {

    companion object {
        private const val DO_NOT_NOTIFY_AGAIN_KEY = "check.google.network"

        private const val ANNOUNCEMENT_URL = "https://github.com/YiiGuxing/TranslationPlugin/discussions/2315"
    }

    override fun onBeforeRunActivity(project: Project): Boolean {
        // 简单判断一下中文环境就可以了...
        return Locale.getDefault() == Locale.CHINA &&
                !service<GoogleSettings>().customServer &&
                !Notifications.isDoNotShowAgain(DO_NOT_NOTIFY_AGAIN_KEY)
    }

    override fun onRunActivity(project: Project) {
        val projectRef = DisposableRef.create(TranslationUIManager.disposable(project), project)
        runAsync { TKK.testConnection() }
            .successOnUiThread(projectRef, ModalityState.NON_MODAL) { p, res ->
                if (!p.isDisposed && !res) {
                    showNotification(p)
                }
            }
    }

    private fun showNotification(project: Project) {
        Notifications.showFullContentNotification(
            "Google翻译引擎和语音朗读功能似乎不可用？",
            "2022年10月1日，Google突然停止了Google翻译在中国大陆的业务，不再向中国大陆区域提供翻译服务，" +
                    "官方给出的理由是“因为使用率低”。这一变化直接不可避免地影响到了插件内置的Google翻译引擎和基于Google翻译的" +
                    "语音朗读（TTS）功能，导致其无法正常使用。因此建议大家换用其他翻译引擎，有道翻译、百度翻译和阿里翻译" +
                    "都是很不错的选择。也可以通过配置Google翻译服务器以继续获取相关服务。" +
                    "未来开发者会带来更多新的翻译引擎和语音合成（TTS）引擎供大家选择，敬请期待！",
            project = project,
        ) {
            it.addAction(ConfigGoogleServerNotificationAction())
            it.addAction(Notifications.BrowseUrlAction("去吐槽", ANNOUNCEMENT_URL))
            it.addAction(Notifications.DoNotShowAgainAction(DO_NOT_NOTIFY_AGAIN_KEY))
        }
    }

    @Suppress("DialogTitleCapitalization")
    private class ConfigGoogleServerNotificationAction : NotificationAction("配置服务器") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()
            TranslationEngine.GOOGLE.showConfigurationDialog()
        }
    }
}