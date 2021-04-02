package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.adaptedMessage
import com.intellij.codeInsight.documentation.render.DocRenderPassFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocCommentBase
import com.intellij.util.concurrency.AppExecutorUtil
import icons.Icons

internal class TranslateRenderedDocAction(
    val editor: Editor,
    val docComment: PsiDocCommentBase
) : ToggleAction({ adaptedMessage("translate") }, Icons.Translation) {

    override fun isSelected(event: AnActionEvent): Boolean {
        return TranslatedDocComments.isTranslated(docComment)
    }

    override fun setSelected(event: AnActionEvent, value: Boolean) {
        val file = docComment.containingFile ?: return

        TranslatedDocComments.setTranslated(docComment, value)

        val project = file.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        ReadAction.nonBlocking<DocRenderPassFactory.Items> {
            DocRenderPassFactory.calculateItemsToRender(editor, file)
        }.finishOnUiThread(ModalityState.current()) {
            DocRenderPassFactory.applyItemsToRender(editor, project, it, false)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

}
