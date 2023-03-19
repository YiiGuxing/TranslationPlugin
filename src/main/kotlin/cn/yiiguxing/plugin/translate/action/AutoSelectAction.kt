package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

/**
 * Auto select action.
 */
abstract class AutoSelectAction(
    private val checkSelection: Boolean,
    private val wordPartCondition: CharCondition = DEFAULT_CONDITION
) : UpdateInBackgroundCompatAction(), ImportantTranslationAction {

    protected abstract val selectionMode: SelectionMode

    /**
     * 更新Action
     *
     * @param e 事件
     */
    protected open fun onUpdate(e: AnActionEvent): Boolean = true

    /**
     * 执行操作
     *
     * @param event          事件
     * @param editor         编辑器
     * @param selectionRange 取词的范围
     */
    protected open fun onActionPerformed(event: AnActionEvent, editor: Editor, selectionRange: TextRange) {}

    protected open val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

    override fun update(e: AnActionEvent) {
        val active = e.editor?.run {
            if (checkSelection && selectionModel.hasSelection()) {
                hasValidSelection()
            } else {
                canPreSelectFromCurrentCaret(wordPartCondition)
            }
        } ?: false
        e.presentation.isEnabledAndVisible = active && onUpdate(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }

        val editor = e.editor ?: return
        e.getSelectionRange()?.takeUnless { it.isEmpty }?.let { onActionPerformed(e, editor, it) }
    }

    private fun Editor.hasValidSelection(): Boolean {
        return selectionModel.selectedText?.filterIgnore()?.any(wordPartCondition) ?: false
    }

    private fun AnActionEvent.getSelectionRange() = editor?.run {
        selectionModel.takeIf { checkSelection && it.hasSelection() }?.run {
            TextRange(selectionStart, selectionEnd)
        } ?: getSelectionFromCurrentCaret(selectionMode, wordPartCondition)
    }
}