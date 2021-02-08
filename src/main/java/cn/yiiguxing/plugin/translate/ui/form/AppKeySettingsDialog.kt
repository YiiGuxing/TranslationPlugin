package cn.yiiguxing.plugin.translate.ui.form

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class AppKeySettingsDialog(title: String, private val appKeySettingsPanel: AppKeySettingsPanel) : DialogWrapper(false) {
    init {
        setTitle(title)
        setResizable(false)
        appKeySettingsPanel.reset()
        init()
    }

    override fun isOK(): Boolean {
        return appKeySettingsPanel.appKeySettings.let { it.appId.isNotEmpty() && it.getAppKey().isNotEmpty() }
    }

    override fun createCenterPanel(): JComponent = appKeySettingsPanel

    override fun doOKAction() {
        appKeySettingsPanel.apply()
        super.doOKAction()
    }
}