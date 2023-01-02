package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.BUNDLE
import cn.yiiguxing.plugin.translate.WebPages
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.jetbrains.annotations.PropertyKey
import java.awt.Desktop
import java.awt.FlowLayout
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.util.*
import javax.swing.*
import javax.swing.event.HyperlinkEvent

/**
 * SupportDialog
 */
class SupportDialog private constructor() : DialogWrapper(null) {

    private val starLink: ActionLink = createActionLink("support.star", GITHUB_URL)
    private val rateLink: ActionLink = createActionLink("support.rate", REVIEWS_URL)
    private val prLink: ActionLink = createActionLink("support.pr", GITHUB_URL)
    private val reportLink: ActionLink = createActionLink("support.report", NEW_ISSUES_URL)
    private val ideaLink: ActionLink = createActionLink("support.feature", IDEA_DISCUSSION_URL)
    private val openCollectiveLink: ActionLink = createActionLink(
        null,
        TranslationIcons.load("/image/donate_to_collective.svg"),
        OPEN_COLLECTIVE_DONATE_URL
    )
    private val donateNoteLink: ActionLink = ActionLink(message("support.label.donate.note")) { showDonatePop(it) }
    private val shareLink: ActionLink = createShareActionLink()
    private val weChatPayLabel: JBLabel = JBLabel(message("support.donate.wechat"))
    private val aliPayLabel: JBLabel = JBLabel(message("support.donate.alipay"))

    init {
        title = message("support")
        setResizable(false)
        setOKButtonText(message("support.thanks"))
        init()
    }

    override fun createCenterPanel(): JComponent = JPanel(VerticalLayout(JBUIScale.scale(8))).apply {
        border = JBUI.Borders.empty(16)
        background = UIManager.getColor("TextArea.background")

        add(JBLabel(message("support.contribution")))
        add(createItemsPanel())
        add(createDonatePanel())
        add(NonOpaquePanel(FlowLayout(FlowLayout.LEFT), donateNoteLink))
    }

    private fun createItemsPanel(): JPanel {
        val layout = MigLayout(LC().gridGap("0!", "0!").insets("0"))
        return NonOpaquePanel(layout).apply {
            var i = 1
            for (item in arrayOf(
                starLink,
                rateLink,
                reportLink,
                ideaLink,
                prLink,
                shareLink,
                JBLabel(message("support.donate"))
            )) {
                add(JBLabel("${i++}. "))
                add(item, UI.wrap())
            }
        }
    }

    private fun createDonatePanel(): JPanel {
        val gap = "${JBUIScale.scale(8)}!"
        val padding = "${JBUIScale.scale(4)} ${JBUIScale.scale(12)}"
        val layout = MigLayout(LC().gridGap(gap, gap).insets(padding))
        return NonOpaquePanel(layout).apply {
            weChatPayLabel.apply {
                horizontalTextPosition = SwingConstants.CENTER
                verticalTextPosition = SwingConstants.BOTTOM
                icon = TranslationIcons.load("/image/donating_wechat_pay.svg")
            }
            aliPayLabel.apply {
                horizontalTextPosition = SwingConstants.CENTER
                verticalTextPosition = SwingConstants.BOTTOM
                icon = TranslationIcons.load("/image/donating_alipay.svg")
            }

            add(ListItemCircleComponent())
            add(openCollectiveLink, UI.spanX(2).wrap())
            add(ListItemCircleComponent())
            add(weChatPayLabel)
            add(aliPayLabel, UI.wrap())
        }
    }


    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction)

    private fun showDonatePop(component: JComponent) {
        val content = message("support.donate.note")
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(content, null, BALLOON_FILL_COLOR) {
                if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    when (it.description) {
                        "#e-mail" -> mail()
                        "#patrons" -> BrowserUtil.browse(WebPages.donors())
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
            .show(RelativePoint(component, Point(component.width / 2, 0)), Balloon.Position.above)
    }

    private fun mail() {
        val content = when (Locale.getDefault().language) {
            Locale.CHINESE.language -> "名字/昵称<网站>：您的留言\n\n" +
                    "捐赠金额：<金额>\n" +
                    "支付平台：支付宝/微信支付\n" +
                    "账单号（后5位）：<账单号>\n\n"

            else -> "Name/Nickname<website>: <message>\n\n" +
                    "Donation Amount: <amount>\n" +
                    "Payment Platform: Alipay/WeChat Pay\n" +
                    "Payment Number (last 5 digits): <number>\n\n"
        }
        val uri = UrlBuilder("mailto:yii.guxing@outlook.com")
            .addQueryParameter("subject", "Donate")
            .addQueryParameter("body", content)
            .build()
        Desktop.getDesktop().mail(URI(uri))
    }

    companion object {
        private const val GITHUB_URL = "https://github.com/YiiGuxing/TranslationPlugin"
        private const val NEW_ISSUES_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new/choose"
        private const val IDEA_DISCUSSION_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/discussions/categories/ideas"
        private const val SUPPORT_SHARE_URL = "https://plugins.jetbrains.com/plugin/8579-translation"
        private const val OPEN_COLLECTIVE_DONATE_URL = "https://opencollective.com/translation-plugin/donate"
        private const val REVIEWS_URL = "https://plugins.jetbrains.com/plugin/8579-translation/reviews"

        private val BALLOON_FILL_COLOR = JBColor(0xE4E6EB, 0x45494B)

        private fun createActionLink(@PropertyKey(resourceBundle = BUNDLE) textKey: String, url: String): ActionLink {
            return createActionLink(message(textKey), null, url)
        }

        private fun createActionLink(text: String?, icon: Icon?, url: String): ActionLink {
            return ActionLink(text, icon) { BrowserUtil.browse(url) }
        }

        private fun createShareActionLink(): ActionLink {
            return ActionLink(message("support.share")) {
                CopyPasteManager.getInstance().setContents(StringSelection(SUPPORT_SHARE_URL))
                JBPopupFactory.getInstance().apply {
                    createHtmlTextBalloonBuilder(message("support.share.notification"), MessageType.INFO, null)
                        .createBalloon()
                        .show(RelativePoint(it, Point(it.width, it.height / 2)), Balloon.Position.atRight)
                }
            }
        }

        fun show() = SupportDialog().show()
    }

}