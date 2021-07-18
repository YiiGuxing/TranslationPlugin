package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.adaptedMessage
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.codeInsight.documentation.render.DocRenderPassFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocCommentBase
import com.intellij.util.concurrency.AppExecutorUtil
import icons.Icons

internal class TranslateRenderedDocAction(
    val editor: Editor,
    private val docComment: PsiDocCommentBase
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
            //if doc rendering is disabled, calculated items have textToRender=null
            withDocRenderingEnabled {
                DocRenderPassFactory.calculateItemsToRender(editor, file)
            }
        }.finishOnUiThread(ModalityState.current()) {
            DocRenderPassFactory.applyItemsToRender(editor, project, it, false)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * Items returned by [DocRenderPassFactory.calculateItemsToRender] contain text only if [DocRenderManager.isDocRenderingEnabled] is true.
     * [DocRenderManager.setDocRenderingEnabled] may be invoked only in UI thread.
     * Using [EditorSettingsExternalizable.setDocCommentRenderingEnabled] works in 2020.3, but in 2021.1 it updates some listeners and
     * requires UI thread as well. The only way I see to fix it now is via reflection.
     */
    @Suppress("KDocUnresolvedReference")
    private fun <T> withDocRenderingEnabled(computation: () -> T): T {
        val key = docRenderEnabledKey() ?: return computation.invoke()
        val existing = editor.getUserData(key)
        editor.putUserData(key, true)
        try {
            return computation.invoke()
        } finally {
            editor.putUserData(key, existing)
        }
    }

    private fun docRenderEnabledKey(): Key<Boolean>? {
        return try {
            val docRenderManagerClass = DocRenderManager::class.java
            val field = docRenderManagerClass.getDeclaredField("DOC_RENDER_ENABLED")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val key = field.get(null) as? Key<Boolean>
            field.isAccessible = false
            key
        } catch (e: Exception) {
            null
        }
    }
}
