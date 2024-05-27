package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import javax.swing.SwingUtilities

class AppKeySettingsDialog(
    title: String,
    private val appKeySettingsPanel: AppKeySettingsPanel,
    private val helpTopic: HelpTopic? = null
) : DialogWrapper(false) {
    init {
        setTitle(title)
        isResizable = false
        Disposer.register(disposable, appKeySettingsPanel)
        init()
    }

    override fun getHelpId(): String? = helpTopic?.id

    override fun isOK(): Boolean = appKeySettingsPanel.isFulfilled

    override fun createCenterPanel(): JComponent = appKeySettingsPanel

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater { appKeySettingsPanel.reset() }
        super.show()
    }

    override fun doOKAction() {
        appKeySettingsPanel.apply()
        super.doOKAction()
    }
}