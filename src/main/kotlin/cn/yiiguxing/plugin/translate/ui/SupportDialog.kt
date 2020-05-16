package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.form.SupportForm
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import org.apache.http.client.utils.URIBuilder
import java.awt.Desktop
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent

/**
 * SupportDialog
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
        openCollectiveLinkLabel.init(OPEN_COLLECTIVE_URL, false)

        donateLinkLabel.icon = null
        donateLinkLabel.setListener({ _, _ -> showDonatePop(donateLinkLabel) }, SUPPORT_PATRONS_URL)
    }

    private fun LinkLabel<String>.init(url: String, cleanIcon: Boolean = true) {
        if (cleanIcon) {
            icon = null
        }
        setListener({ _, linkUrl -> BrowserUtil.browse(linkUrl) }, url)
    }

    private fun showDonatePop(component: JComponent) {
        @Suppress("SpellCheckingInspection")
        val content = """
            使用支付宝/微信支付捐赠后请留言或者通过邮件提供您的名字/昵称和网站，格式为：<br/>
            <i><b>名字/昵称 [&lt;网站>][：留言]</i></b><br/>
            网站与留言为可选部分，以下是一个例子：<br/>
            <i><b>Yii.Guxing &lt;github.com/YiiGuxing>：加油！</i></b><br/>
            通过邮件发送时，请还提供以下信息：<br/><i><b>
            捐赠金额：
            支付平台：支付宝/微信支付
            支付宝用户名/微信用户名/单号（后5位）：</i></b><br/>
            邮箱地址：<a href="#e-mail"><b>yii.guxing@gmail.com</b></a> (点击发送邮件)<br/>
            您提供的名字、网站和捐赠总额将会被添加到<a href="#patrons"><b>Patrons/捐赠者</b></a>列表中，列表将按捐赠总额列出前50名捐赠者。<br/>
            感谢您的慷慨捐赠！
        """.trimIndent()
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(content, null, BALLOON_FILL_COLOR) {
                if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    if (it.description == "#e-mail") {
                        mail()
                    } else {
                        BrowserUtil.browse(SUPPORT_PATRONS_URL)
                    }
                }
            }
            .setShadow(true)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnFrameResize(true)
            .setHideOnKeyOutside(true)
            .setHideOnLinkClick(true)
            .setDisposable(disposable)
            .setContentInsets(JBUI.insets(10))
            .createBalloon()
            .show(JBPopupFactory.getInstance().guessBestPopupLocation(component), Balloon.Position.above)
    }

    private fun mail() {
        val uri = URIBuilder()
            .setScheme("mailto")
            .setPath("yii.guxing@gmail.com")
            .setParameter("subject", "Donate")
            .setParameter("body", "名字/昵称<网站>：您的留言\n\n捐赠金额：\n支付平台：支付宝/微信支付\n支付宝用户名/微信用户名/单号（后5位）：\n\n")
            .build()
        Desktop.getDesktop().mail(uri)
    }

    override fun createCenterPanel(): JComponent = form.rootPane

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction)

    companion object {
        private val BALLOON_FILL_COLOR = JBColor(0xE4E6EB, 0x45494B)

        fun show() = SupportDialog().show()
    }

}