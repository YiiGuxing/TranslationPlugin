package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HelpTopic
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ApiSettingsDialog(
    title: String,
    private val apiSettingsPanel: ApiSettingsPanel,
    private val helpTopic: HelpTopic? = null
) : DialogWrapper(false) {
    init {
        setTitle(title)
        isResizable = false
        Disposer.register(disposable, apiSettingsPanel)
        init()
    }

    override fun getHelpId(): String? = helpTopic?.id

    override fun isOK(): Boolean = apiSettingsPanel.isFulfilled

    override fun createCenterPanel(): JComponent = apiSettingsPanel

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater { apiSettingsPanel.reset() }
        super.show()
    }

    override fun doOKAction() {
        apiSettingsPanel.apply()
        super.doOKAction()
    }
}