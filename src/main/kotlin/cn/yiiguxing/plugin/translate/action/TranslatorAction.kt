package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Condition

/**
 * TranslatorAction
 */
class TranslatorAction(private val translator: Translator) :
    DumbAwareAction(translator.name, null, translator.icon) {

    override fun actionPerformed(e: AnActionEvent) {
        Settings.translator = translator.id
    }

    companion object {
        val ACTIONS: List<TranslatorAction> = TranslateService.getTranslators().map { TranslatorAction(it) }

        val PRESELECT_CONDITION: Condition<AnAction> = Condition { action ->
            (action as? TranslatorAction)?.translator?.id == Settings.translator
        }
    }
}