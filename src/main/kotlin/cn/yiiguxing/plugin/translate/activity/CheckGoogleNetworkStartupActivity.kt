package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.trans.google.TKK
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.successOnUiThread
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import org.jetbrains.concurrency.runAsync
import java.util.*

class CheckGoogleNetworkStartupActivity : BaseStartupActivity() {

    override fun onRunActivity(project: Project) {
        // 简单判断一下中文环境就可以了...
        if (Locale.getDefault() != Locale.CHINA ||
            TranslateService.translator.id != TranslationEngine.GOOGLE.id ||
            Notifications.isDoNotShowAgain(DO_NOT_NOTIFY_AGAIN_KEY)
        ) {
            return
        }

        val projectRef = DisposableRef.create(TranslationUIManager.disposable(project), project)
        runAsync { if (testBlog()) TKK.testConnection() else null }
            .successOnUiThread(projectRef, ModalityState.NON_MODAL) { p, res ->
                if (!p.isDisposed && res == false) {
                    showNotification(p)
                }
            }
    }

    companion object {
        private const val DO_NOT_NOTIFY_AGAIN_KEY = "check.google.network"

        private const val FIX_GOOGLE_URL = "https://yiiguxing.gitee.io/translation-plugin/fix-google"

        private fun testBlog(): Boolean = try {
            HttpRequests.request(FIX_GOOGLE_URL)
                .connectTimeout(5000)
                .throwStatusCodeException(true)
                .tryConnect()
            true
        } catch (e: Throwable) {
            false
        }

        private fun showNotification(project: Project) {
            Notifications.showNotification(
                TranslationPlugin.descriptor.name,
                "当前的翻译引擎似乎不可用？",
                NotificationType.INFORMATION,
                project,
                notificationCustomizer = {
                    it.addAction(Notifications.BrowseUrlAction("查看解决办法", FIX_GOOGLE_URL))
                    it.addAction(Notifications.DoNotShowAgainAction(DO_NOT_NOTIFY_AGAIN_KEY))
                }
            )
        }
    }

}