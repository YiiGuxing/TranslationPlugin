package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.TKK
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import java.util.*

class CheckGoogleNetworkStartupActivity : BaseStartupActivity() {

    override fun onBeforeRunActivity(project: Project): Boolean {
        // 简单判断一下中文环境就可以了...
        return Locale.getDefault() == Locale.CHINA &&
                TranslateService.translator.id == GoogleTranslator.id &&
                !PropertiesComponent.getInstance().getBoolean(DO_NOT_NOTIFY_AGAIN_PROPERTY, false)
    }

    override fun onRunActivity(project: Project) {
        executeOnPooledThread {
            if (!TKK.testConnection()) {
                invokeLater(ModalityState.NON_MODAL) {
                    if (!project.isDisposed) {
                        showNotification(project)
                    }
                }
            }
        }
    }

    companion object {
        private const val ANNOUNCEMENT_URL = "https://github.com/YiiGuxing/TranslationPlugin/discussions/2315"

        private const val DISPLAY_ID = "Check Google Network"

        private const val DO_NOT_NOTIFY_AGAIN_PROPERTY = "yii.guxing.translate.check.google.network"

        private fun showNotification(project: Project) {
            NotificationGroup(DISPLAY_ID, NotificationDisplayType.BALLOON, true)
                .createNotification(
                    "当前翻译引擎似乎不可用？",
                    "2022年10月1日，Google突然停止了Google翻译在中国大陆的业务，不再向中国大陆区域提供翻译服务，" +
                            "官方给出的理由是“因为使用率低”。这一变化直接不可避免地影响到了插件内置的Google翻译引擎和基于Google翻译的" +
                            "语音朗读（TTS）功能，导致其无法正常使用。因此建议大家换用其他翻译引擎，有道翻译、百度翻译和阿里翻译" +
                            "都是很不错的选择。未来开发者会带来更多新的翻译引擎和语音合成（TTS）引擎供大家选择，敬请期待！",
                    NotificationType.INFORMATION,
                    null
                )
                .addAction(BrowseUrlAction("去吐槽", ANNOUNCEMENT_URL))
                .addAction(DoNotShowAgainAction())
                .setImportant(true)
                .show(project)
        }
    }

    class BrowseUrlAction(
        text: String?,
        private val url: String,
        private val expireNotification: Boolean = true
    ) : NotificationAction(text) {

        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            if (expireNotification) {
                notification.expire()
            }
            BrowserUtil.browse(url)
        }
    }

    private class DoNotShowAgainAction : NotificationAction("不再提示") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()
            PropertiesComponent.getInstance().setValue(DO_NOT_NOTIFY_AGAIN_PROPERTY, true)
        }
    }
}