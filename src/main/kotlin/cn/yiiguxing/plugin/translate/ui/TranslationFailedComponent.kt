package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.action.SwitchTranslatorAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.ErrorInfo
import cn.yiiguxing.plugin.translate.trans.TranslateException
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import cn.yiiguxing.plugin.translate.util.copyToClipboard
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.laf.darcula.ui.DarculaOptionButtonUI
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBOptionButton
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.Icons
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel

class TranslationFailedComponent : JPanel() {

    private val errorInfo: JBLabel = JBLabel()

    private val switchTranslatorAction =
        object : AbstractAction(message("translation.failed.component.action.switch.translator")) {
            override fun actionPerformed(e: ActionEvent) {
                doSwitchTranslator()
            }
        }

    private var retryHandler: (() -> Unit)? = null

    private val optionButton: JBOptionButton = JBOptionButton(switchTranslatorAction, emptyArray())

    init {
        init()
        initIconAndMessage()
        initErrorInfo()
        initButtons()
    }

    private fun init() {
        val layoutConstraints = LC()
            .align("center", "center")
            .gridGap("0!", "0!")
            .insets(JBUIScale.scale(INSETS).toString())
        layout = MigLayout(layoutConstraints)
    }

    private fun initIconAndMessage() {
        add(JBLabel(AllIcons.General.ErrorDialog), cc())
        add(
            JBLabel(message("translation.failed.component.message")).apply {
                font = JBFont.label().deriveFont(JBUIScale.scale(20f))
            },
            cc().gapTop(JBUIScale.scale(10).toString())
        )
    }

    private fun initErrorInfo() {
        errorInfo.apply {
            isVisible = false
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
        }

        val cc = cc()
            .gapTop(JBUIScale.scale(4).toString())
            .gapBottom(JBUIScale.scale(12).toString())
        add(errorInfo, cc)
    }

    private fun initButtons() {
        val buttonsPanel = JPanel().apply {
            layout = HorizontalLayout(JBUIScale.scale(4))
            add(JButton(message("translation.failed.component.action.retry")).apply {
                addActionListener { retryHandler?.invoke() }
            })
            add(optionButton)
        }
        add(buttonsPanel, cc())
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        errorInfo.maximumSize = Dimension(width - JBUIScale.scale(INSETS * 10), Int.MAX_VALUE)
    }

    private fun doSwitchTranslator() {
        val dataContext = DataManager.getInstance().getDataContext(optionButton)
        if (SwitchTranslatorAction.canSwitchTranslatorQuickly()) {
            val offset = JBUIScale.scale(3)
            val offsetX = if (UIUtil.isUnderDarcula() || optionButton.ui is DarculaOptionButtonUI) offset else 0
            SwitchTranslatorAction
                .createTranslatorPopup(dataContext)
                .apply { minimumSize = Dimension(optionButton.width - offsetX * 2, 1) }
                .showBelow(optionButton, offsetX, offset)
        } else {
            OptionsConfigurable.showSettingsDialog()
        }
    }

    fun onRetry(handler: () -> Unit) {
        retryHandler = handler
    }

    fun update(throwable: Throwable?) {
        val errorInfo = (throwable as? TranslateException)?.errorInfo
        val errorMessage = errorInfo?.message
        this.errorInfo.apply {
            text = errorMessage
            toolTipText = errorMessage
            isVisible = !errorMessage.isNullOrEmpty()
        }
        optionButton.setOptions(createOptions(errorInfo, throwable))
    }

    private fun createOptions(errorInfo: ErrorInfo?, throwable: Throwable?): List<AnAction> {
        val options = mutableListOf<AnAction>()
        if (errorInfo != null) {
            options += errorInfo.continueActions
        }
        if (throwable != null) {
            options += object :
                AnAction(message("translation.failed.component.action.copy.error.info"), null, Icons.RecordErrorInfo) {
                override fun actionPerformed(e: AnActionEvent) {
                    val errorMessage = errorInfo?.message ?: message("error.unknown")
                    val message = message("error.translate.failed", errorMessage)
                    throwable.copyToClipboard(message)

                    // TODO copy and send feedback
                }
            }
        }

        return options
    }

    companion object {
        private const val INSETS = 10

        private fun cc(): CC = CC().alignX("center").wrap()
    }
}