package cn.yiiguxing.plugin.translate.ui.form

import cn.yiiguxing.plugin.translate.AppKeySettings
import cn.yiiguxing.plugin.translate.action.BrowseAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.wrap
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.labels.ActionLink
import com.intellij.util.ui.JBUI
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class AppKeySettingsPanel(logoImage: Icon, appKeyLink: String, val appKeySettings: AppKeySettings) : JPanel() {
    private val appIdField: JBTextField = JBTextField()
    private val appKeyField: JBPasswordField = JBPasswordField()

    private val logo: JLabel = JLabel(logoImage)
    private val getApiKeyLink: ActionLink =
        ActionLink(message("settings.link.getAppKey"), AllIcons.Ide.Link, BrowseAction(appKeyLink))

    init {
        layout = migLayout()

        logo.border = JBUI.Borders.empty(0, 0, 10, 0)
        add(logo, wrap().span(2).alignX("50%"))

        add(JLabel(message("settings.label.appId")))
        add(appIdField, fillX().wrap())

        add(JLabel(message("settings.label.appPrivateKey")))
        add(appKeyField, fillX().wrap())

        getApiKeyLink.border = JBUI.Borders.empty(10, 0, 0, 0)
        add(getApiKeyLink, wrap().span(2))
    }

    private var appKey: String?
        get() = appKeyField.password
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it) }
            ?: ""
        set(value) {
            appKeyField.text = if (value.isNullOrEmpty()) null else value
        }

    fun reset() {
        appIdField.text = appKeySettings.appId
        appKey = appKeySettings.getAppKey()
    }

    fun apply() {
        appKeySettings.appId = appIdField.text
        appKeySettings.setAppKey(appKey)
    }
}