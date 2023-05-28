package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatComboBoxAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Condition
import javax.swing.JComponent


/**
 * Switch translator action
 */
class SwitchTranslationEngineAction : UpdateInBackgroundCompatComboBoxAction(), DumbAware, PopupAction {

    init {
        setPopupTitle(message("translation.engines.popup.title"))
        isEnabledInModalContext = true
        templatePresentation.text = message("action.SwitchTranslationEngineAction.text")
        templatePresentation.description = message("action.SwitchTranslationEngineAction.description")
    }

    override fun update(e: AnActionEvent) {
        TranslateService.translator.let {
            e.presentation.text = it.name
            e.presentation.icon = it.icon
        }
    }

    override fun getPreselectCondition(): Condition<AnAction> = TranslationEngineAction.PRESELECT_CONDITION

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup = TranslationEngineActionGroup()

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton {
        return object : ComboBoxButton(presentation) {
            override fun createPopup(onDispose: Runnable?): JBPopup {
                val originalPopupTitle = myPopupTitle
                myPopupTitle = ""
                val popup = super.createPopup(onDispose)
                myPopupTitle = originalPopupTitle
                return popup
            }
        }
    }

    override fun shouldShowDisabledActions(): Boolean = true


    companion object {
        fun createTranslationEnginesPopup(
            context: DataContext,
            title: String? = message("translation.engines.popup.title"),
            disposeCallback: Runnable? = null
        ): ListPopup = TranslationEngineActionGroup().createActionPopup(context, title, disposeCallback)
    }
}