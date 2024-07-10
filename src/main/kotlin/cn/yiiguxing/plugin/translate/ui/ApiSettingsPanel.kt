package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ApiSettings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.wrap
import cn.yiiguxing.plugin.translate.util.DisposableRef
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

class ApiSettingsPanel(
    logoImage: Icon,
    appKeyLink: String,
    private val apiSettings: ApiSettings
) : JPanel(), Disposable {
    private val apiEndpointField: JBTextField = JBTextField()
    private val apiKeyField: JBPasswordField = JBPasswordField()

    private val logo: JLabel = JLabel(logoImage)
    private val getApiKeyLink: ActionLink =
        ActionLink(message("settings.link.getAppKey"), AllIcons.Ide.Link, AllIcons.Ide.Link) {
            BrowserUtil.browse(appKeyLink)
        }

    private var isApiKeySet: Boolean = false

    val isFulfilled: Boolean get() = isApiKeySet && apiSettings.apiEndpoint.isNotEmpty()

    init {
        layout = migLayout()
        minimumSize = JBUI.size(300, 0)

        logo.border = JBUI.Borders.empty(10, 0, 18, 0)
        add(logo, wrap().span(2).alignX("50%"))

        val gap = JBUIScale.scale(8).toString()
        add(JLabel(message("settings.label.apiEndpoint")))
        add(apiEndpointField, fillX().gapLeft(gap).wrap())

        add(JLabel(message("settings.label.apiKey")))
        add(apiKeyField, fillX().gapLeft(gap).wrap())

        getApiKeyLink.border = JBUI.Borders.emptyTop(10)
        add(getApiKeyLink, wrap().span(2))
    }

    private var apiKey: String?
        get() = apiKeyField.password?.let { String(it) }
        set(value) {
            apiKeyField.text = value
        }

    fun reset() {
        apiEndpointField.text = apiSettings.apiEndpoint
        apiKey = ""
        val ref = DisposableRef.create(this, this)
        runAsync { apiSettings.getApiKey() to apiSettings.isApiKeySet }
            .expireWith(this)
            .successOnUiThread(ref) { panel, (key, isAppKeySet) ->
                panel.apiKey = key
                panel.isApiKeySet = isAppKeySet
            }
            .disposeAfterProcessing(ref)
    }

    fun apply() {
        apiSettings.apiEndpoint = apiEndpointField.text.trim()
        apiSettings.setApiKey(apiKey)
        isApiKeySet = apiSettings.isApiKeySet
    }

    override fun dispose() {
    }
}