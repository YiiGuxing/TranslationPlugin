package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.AppKeySettings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.wrap
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import org.jetbrains.concurrency.runAsync
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class AppKeySettingsPanel(
    logoImage: Icon,
    appKeyLink: String,
    private val appKeySettings: AppKeySettings
) : JPanel(), Disposable {
    private val appIdField: JBTextField = JBTextField()
    private val appKeyField: JBPasswordField = JBPasswordField()

    private val logo: JLabel = JLabel(logoImage)
    private val getApiKeyLink: ActionLink =
        ActionLink(message("settings.link.getAppKey"), AllIcons.Ide.Link, AllIcons.Ide.Link) {
            BrowserUtil.browse(appKeyLink)
        }

    private var isAppKeySet: Boolean = false

    val isFulfilled: Boolean get() = isAppKeySet && appKeySettings.appId.isNotEmpty()

    init {
        layout = migLayout()
        minimumSize = JBUI.size(300, 0)

        logo.border = JBUI.Borders.empty(10, 0, 18, 0)
        add(logo, wrap().span(2).alignX("50%"))

        val gap = JBUIScale.scale(8).toString()
        add(JLabel(message("settings.label.appId")))
        add(appIdField, fillX().gapLeft(gap).wrap())

        add(JLabel(message("settings.label.appPrivateKey")))
        add(appKeyField, fillX().gapLeft(gap).wrap())

        getApiKeyLink.border = JBUI.Borders.emptyTop(10)
        add(getApiKeyLink, wrap().span(2))
    }

    private var appKey: String?
        get() = appKeyField.password?.let { String(it) }
        set(value) {
            appKeyField.text = value
        }

    fun reset() {
        appIdField.text = appKeySettings.appId
        appKey = ""
        val ref = DisposableRef.create(this, this)
        asyncLatch { latch ->
            runAsync {
                latch.await()
                appKeySettings.getAppKey() to appKeySettings.isAppKeySet
            }
                .expireWith(this)
                .successOnUiThread(ref) { panel, (key, isAppKeySet) ->
                    panel.appKey = key
                    panel.isAppKeySet = isAppKeySet
                }
                .disposeAfterProcessing(ref)
        }
    }

    fun apply() {
        appKeySettings.appId = appIdField.text.trim()
        appKeySettings.setAppKey(appKey)
        isAppKeySet = appKeySettings.isAppKeySet
    }

    override fun dispose() {
    }
}