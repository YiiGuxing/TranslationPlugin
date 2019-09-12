package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.form.SupportForm
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.UIManager

/**
 * SupportDialog
 *
 * Created by Yii.Guxing on 2019/09/12.
 */
class SupportDialog : DialogWrapper(null) {

    private val form = SupportForm()

    init {
        title = "支持"
        setOKButtonText("感谢您的支持！")
        form.init()
        init()
    }

    private fun SupportForm.init() {
        rootPane.border = JBUI.Borders.empty(10)
        rootPane.background = UIManager.getColor("TextArea.background")

        starLinkLabel.icon = null
        reportLinkLabel.icon = null
        ideaLinkLabel.icon = null
    }

    override fun createCenterPanel(): JComponent = form.rootPane

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction)

}