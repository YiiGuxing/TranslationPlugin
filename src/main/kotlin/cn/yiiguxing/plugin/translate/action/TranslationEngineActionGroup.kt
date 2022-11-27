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
open class TranslationEngineActionGroup(
    name: Supplier<@NlsActions.ActionText String> = Supplier { message("action.TranslationEngineActionGroup.name") },
    popup: Boolean = true
) : DefaultActionGroup(name, popup) {

    init {
        addAll(translationEngineGroupActions())
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
        title: String? = message("translation.engines.popup.title"),
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
            TranslationEngineAction.PRESELECT_CONDITION
        )


    companion object {
        private fun translationEngineGroupActions(): List<AnAction> {
            val availableActions = TranslationEngineAction.availableActions()
            val unavailableActions = TranslationEngineAction.unavailableActions()

            val actions = ArrayList<AnAction>(availableActions.size + unavailableActions.size + 3)
            actions.addAll(availableActions)
            if (unavailableActions.isNotEmpty()) {
                actions.add(Separator.create(message("action.TranslationEngineActionGroup.separator.inactivated")))
                actions.addAll(unavailableActions)
                actions.add(Separator.create())
                actions.add(SettingsAction(message("action.TranslationEngineActionGroup.manage.translators"), null))
            }

            return actions
        }
    }

}