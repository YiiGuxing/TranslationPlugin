package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.action.SwitchTranslationEngineAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslateException
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.laf.darcula.ui.DarculaOptionButtonUI
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBOptionButton
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
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

    private val switchTranslationEngineAction =
        object : AbstractAction(message("translation.failed.component.action.switch.translation.engine")) {
            override fun actionPerformed(e: ActionEvent) {
                doSwitchTranslationEngine()
            }
        }

    private var retryHandler: (() -> Unit)? = null

    private val optionButton: JBOptionButton = JBOptionButton(switchTranslationEngineAction, emptyArray())

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
        errorInfo.maximumSize = Dimension(width - JBUIScale.scale(INSETS * 4), Int.MAX_VALUE)
    }

    private fun doSwitchTranslationEngine() {
        val dataContext = DataManager.getInstance().getDataContext(optionButton)
        val offset = JBUIScale.scale(3)
        val offsetX = if (UIUtil.isUnderDarcula() || optionButton.ui is DarculaOptionButtonUI) offset else 0
        SwitchTranslationEngineAction.createTranslationEnginesPopup(dataContext)
            .apply { minimumSize = Dimension(optionButton.width - offsetX * 2, 1) }
            .showBelow(optionButton, offsetX, offset)
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

        optionButton.setOptions(errorInfo?.continueActions)
    }


    companion object {
        private const val INSETS = 10

        private fun cc(): CC = CC().alignX("center").wrap()
    }
}