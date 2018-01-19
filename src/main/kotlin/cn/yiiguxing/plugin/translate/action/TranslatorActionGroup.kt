package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/**
 * TranslatorActionGroup
 *
 * Created by Yii.Guxing on 2018/01/19
 */
class TranslatorActionGroup : ActionGroup() {

    private val translatorActions: Array<AnAction> = getActions().toTypedArray()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = translatorActions

    companion object {
        const val TITLE = "Translators"

        fun getActions(): List<AnAction> = TranslateService.getTranslators()
                .map { translator ->
                    object : DumbAwareAction(translator.name, null, translator.icon) {
                        override fun actionPerformed(e: AnActionEvent) {
                            Settings.translator = translator.id
                        }
                    }
                }
    }
}