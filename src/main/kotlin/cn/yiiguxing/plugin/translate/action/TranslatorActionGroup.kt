package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.NlsActions
import java.util.function.Supplier

/**
 * TranslatorActionGroup
 */
open class TranslatorActionGroup(
    name: Supplier<@NlsActions.ActionText String> = Supplier { message("action.TranslatorActionGroup.name") },
    popup: Boolean = true
) : DefaultActionGroup(name, popup) {

    init {
        addAll(translatorGroupActions())
    }

    override fun isDumbAware(): Boolean = true

    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        val popup = createActionPopup(dataContext)
        PlatformCoreDataKeys.CONTEXT_COMPONENT
            .getData(dataContext)
            ?.let { component -> popup.showUnderneathOf(component) }
            ?: popup.showInBestPositionFor(dataContext)
    }

    fun createActionPopup(
        context: DataContext,
        title: String? = message("translator.popup.title"),
        disposeCallback: Runnable? = null
    ): ListPopup = JBPopupFactory
        .getInstance()
        .createActionGroupPopup(
            title,
            this,
            context,
            false,
            true,
            false,
            disposeCallback,
            10,
            TranslatorAction.PRESELECT_CONDITION
        )


    companion object {
        private fun translatorGroupActions(): List<AnAction> {
            val availableActions = TranslatorAction.availableActions()
            val unavailableActions = TranslatorAction.unavailableActions()

            val actions = ArrayList<AnAction>(availableActions.size + unavailableActions.size + 3)
            actions.addAll(availableActions)
            if (unavailableActions.isNotEmpty()) {
                actions.add(Separator.create(message("action.TranslatorActionGroup.separator.inactivated")))
                actions.addAll(unavailableActions)
                actions.add(Separator.create())
                actions.add(SettingsAction(message("action.TranslatorActionGroup.go.to.activate"), null))
            }

            return actions
        }
    }

}