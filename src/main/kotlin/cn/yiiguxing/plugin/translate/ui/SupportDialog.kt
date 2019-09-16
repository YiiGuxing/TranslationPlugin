package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.GITHUB_URL
import cn.yiiguxing.plugin.translate.NEW_ISSUES_URL
import cn.yiiguxing.plugin.translate.OPEN_COLLECTIVE_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.SupportForm
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.UIManager

/**
 * SupportDialog
 *
 * Created by Yii.Guxing on 2019/09/12.
 */
class SupportDialog private constructor() : DialogWrapper(null) {

    private val form = SupportForm()

    init {
        title = message("support")
        setOKButtonText(message("support.thanks"))
        form.init()
        init()
    }

    private fun SupportForm.init() {
        rootPane.border = JBUI.Borders.empty(12, 15)
        rootPane.background = UIManager.getColor("TextArea.background")

        starLinkLabel.init(GITHUB_URL)
        prLinkLab.init(GITHUB_URL)
        reportLinkLabel.init(NEW_ISSUES_URL)
        ideaLinkLabel.init(NEW_ISSUES_URL)
        openCollectiveLinkLabel.init(OPEN_COLLECTIVE_URL)
    }

    private fun LinkLabel<String>.init(url: String) {
        icon = null
        setListener({ _, linkUrl -> BrowserUtil.browse(linkUrl) }, url)
    }

    override fun createCenterPanel(): JComponent = form.rootPane

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction)

    companion object {
        fun show() = SupportDialog().show()
    }

}