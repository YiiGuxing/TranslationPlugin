package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

/**
 * AutoSelectAction
 *
 * Created by Yii.Guxing on 2017/9/12
 */
abstract class AutoSelectAction(
        private val checkSelection: Boolean,
        private val wordPartCondition: CharCondition = DEFAULT_CONDITION
) : AnAction() {

    protected abstract val selectionMode: SelectionMode

    /**
     * 更新Action
     *
     * @param e      事件
     * @param active 是否活动的，表示是否可以取到词
     */
    protected open fun onUpdate(e: AnActionEvent, active: Boolean) {}

    /**
     * 执行操作
     *
     * @param e              事件
     * @param editor         编辑器
     * @param selectionRange 取词的范围
     */
    protected open fun onActionPerformed(e: AnActionEvent, editor: Editor, selectionRange: TextRange) {}

    protected open val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

    override fun update(e: AnActionEvent) {
        val active = e.editor?.let { editor ->
            checkSelection && editor.selectionModel.hasSelection() || editor.canSelect()
        } ?: false

        onUpdate(e, active)
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }

        val editor = e.editor ?: return
        e.getSelectionRange()?.takeUnless { it.isEmpty }?.let { onActionPerformed(e, editor, it) }
    }

    private fun Editor.canSelect(): Boolean {
        val offset = caretModel.offset
        val textLength = document.textLength
        if (textLength == 0) {
            return false
        }

        return TextRange(maxOf(0, offset - 1), minOf(textLength, offset + 1))
                .let { document.getText(it) }
                .find(wordPartCondition) != null
    }

    private fun AnActionEvent.getSelectionRange() = editor?.run {
        selectionModel.takeIf { checkSelection && it.hasSelection() }?.run {
            TextRange(selectionStart, selectionEnd)
        } ?: getSelectionFromCurrentCaret(selectionMode, wordPartCondition)
    }
}