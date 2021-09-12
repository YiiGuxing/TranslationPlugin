package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * 翻译动作，自动从最大范围内取词，优先选择
 */
class EditorTranslateAction : TranslateAction(true) {

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.EditorTranslateAction.text")
        templatePresentation.description = message("action.EditorTranslateAction.description")
    }

    override fun onUpdate(e: AnActionEvent): Boolean {
        return (hasEditorSelection(e) || mayTranslateWithNoSelection(e)) && super.onUpdate(e)
    }

    override val selectionMode
        get() = Settings.autoSelectionMode

    companion object {
        const val ACTION_ID = "\$EditorTranslateAction"
    }

}
