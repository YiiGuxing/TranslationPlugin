package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class AppKeySettingsDialog(
    title: String,
    private val appKeySettingsPanel: AppKeySettingsPanel,
    private val helpTopic: HelpTopic? = null
) : DialogWrapper(false) {
    init {
        setTitle(title)
        isResizable = false
        appKeySettingsPanel.reset()
        init()
    }

    override fun getHelpId(): String? = helpTopic?.id

    override fun isOK(): Boolean {
        return appKeySettingsPanel.appKeySettings.let { it.appId.isNotEmpty() && it.getAppKey().isNotEmpty() }
    }

    override fun createCenterPanel(): JComponent = appKeySettingsPanel

    override fun doOKAction() {
        appKeySettingsPanel.apply()
        super.doOKAction()
    }
}