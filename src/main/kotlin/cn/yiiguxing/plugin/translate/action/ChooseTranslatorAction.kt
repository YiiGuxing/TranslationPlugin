package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import javax.swing.JComponent


/**
 * ChooseTranslatorAction
 */
class ChooseTranslatorAction : ComboBoxAction(), DumbAware {

    init {
        setPopupTitle(message("choose.translator.popup.title"))
        isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
        TranslateService.translator.let {
            e.presentation.text = it.name
            e.presentation.icon = it.icon
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        WindowManagerEx.getInstanceEx()
            .findFrameFor(e.project)
            ?.component
            ?.let { createActionPopup(message("choose.translator.popup.title"), e.dataContext, it).showInCenterOf(it) }
    }

    private fun createActionPopup(
        title: String?,
        context: DataContext,
        component: JComponent,
        disposeCallback: Runnable? = null
    ): ListPopup {
        val group = createPopupActionGroup(component, context)
        return JBPopupFactory
            .getInstance()
            .createActionGroupPopup(
                title,
                group,
                context,
                false,
                shouldShowDisabledActions(),
                false,
                disposeCallback,
                maxRows,
                preselectCondition
            )
            .apply { setMinimumSize(Dimension(minWidth, minHeight)) }
    }

    override fun getPreselectCondition(): Condition<AnAction> = TranslatorAction.PRESELECT_CONDITION

    override fun createPopupActionGroup(button: JComponent)
            : DefaultActionGroup = DefaultActionGroup(TranslatorAction.ACTIONS)

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton =
        object : ComboBoxButton(presentation) {
            override fun createPopup(onDispose: Runnable?): JBPopup {
                return createActionPopup(null, dataContext, this, onDispose)
            }
        }
}