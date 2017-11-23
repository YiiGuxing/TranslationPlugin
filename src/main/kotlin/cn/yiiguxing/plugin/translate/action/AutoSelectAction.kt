package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.CharCondition
import cn.yiiguxing.plugin.translate.util.DEFAULT_CONDITION
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.getSelectionFromCurrentCaret
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

    abstract protected val selectionMode: SelectionMode

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

    protected open fun getEditor(e: AnActionEvent): Editor? = CommonDataKeys.EDITOR.getData(e.dataContext)

    override fun update(e: AnActionEvent) {
        val active = getEditor(e)?.let { editor ->
            checkSelection && editor.selectionModel.hasSelection() || canSelect(editor)
        } ?: false

        onUpdate(e, active)
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) return

        getEditor(e)?.let { editor ->
            getSelectionRange(e)?.takeIf { !it.isEmpty }?.let { onActionPerformed(e, editor, it) }
        }
    }

    private fun canSelect(editor: Editor): Boolean {
        val offset = editor.caretModel.offset
        val document = editor.document
        val textLength = document.textLength

        if (textLength == 0) {
            return false
        }

        val text = document.getText(TextRange(maxOf(0, offset - 1), minOf(textLength, offset + 1)))
        for (c in text) {
            if (wordPartCondition.value(c)) {
                return true
            }
        }

        return false
    }

    private fun getSelectionRange(e: AnActionEvent) = getEditor(e)?.run {
        selectionModel.takeIf { checkSelection && it.hasSelection() }?.run {
            TextRange(selectionStart, selectionEnd)
        } ?: getSelectionFromCurrentCaret(selectionMode, wordPartCondition)
    }

}