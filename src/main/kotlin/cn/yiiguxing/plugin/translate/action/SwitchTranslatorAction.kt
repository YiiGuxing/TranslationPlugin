package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import javax.swing.JComponent


/**
 * Switch translator action
 */
class SwitchTranslatorAction : ComboBoxAction(), DumbAware {

    private var lastPopupTitle: String? = null

    init {
        setPopupTitle(message("translator.popup.title"))
        isEnabledInModalContext = true
        templatePresentation.text = message("action.SwitchTranslatorAction.text")
        templatePresentation.description = message("action.SwitchTranslatorAction.description")
    }

    override fun setPopupTitle(popupTitle: String?) {
        super.setPopupTitle(popupTitle)
        lastPopupTitle = popupTitle
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = canSwitchTranslatorQuickly()

        TranslateService.translator.let {
            e.presentation.text = it.name
            e.presentation.icon = it.icon
        }
    }

    override fun getPreselectCondition(): Condition<AnAction> = TranslatorAction.PRESELECT_CONDITION

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup = TranslatorActionGroup()

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton {
        return object : ComboBoxButton(presentation) {
            override fun createPopup(onDispose: Runnable?): JBPopup {
                val originalPopupTitle = lastPopupTitle
                setPopupTitle(null)
                val popup = super.createPopup(onDispose)
                setPopupTitle(originalPopupTitle)
                return popup
            }
        }
    }

    override fun shouldShowDisabledActions(): Boolean = true


    companion object {
        fun canSwitchTranslatorQuickly() = TranslatorAction.availableActions().size > 1

        fun createTranslatorPopup(
            context: DataContext,
            title: String? = message("translator.popup.title"),
            disposeCallback: Runnable? = null
        ): ListPopup = TranslatorActionGroup().createActionPopup(context, title, disposeCallback)
    }
}