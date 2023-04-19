package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.action.FixedIconToggleAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.codeInsight.documentation.render.DocRenderPassFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocCommentBase
import com.intellij.util.concurrency.AppExecutorUtil
import icons.TranslationIcons

internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val docComment: PsiDocCommentBase
) : FixedIconToggleAction(
    TranslationIcons.Documentation,
    { adaptedMessage("action.TranslateRenderedDocAction.text") }
) {

    private val isEnabled: Boolean by lazy { DocTranslationService.isSupportedForPsiElement(docComment) }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = isEnabled
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return DocTranslationService.isInlayDocTranslated(docComment)
    }

    override fun setSelected(event: AnActionEvent, value: Boolean) {
        val editor = editor.takeUnless { it.isDisposed } ?: return
        val file = docComment.containingFile ?: return
        val project = file.project

        DocTranslationService.setInlayDocTranslated(docComment, value)

        ReadAction
            .nonBlocking<DocRenderPassFactory.Items?> {
                try {
                    // if doc rendering is disabled, calculated items have textToRender=null
                    withDocRenderingEnabled {
                        DocRenderPassFactory.calculateItemsToRender(editor, file)
                    }
                } catch (e: Throwable) {
                    LOG.w("Failed to calculate doc items to render", e)
                    null
                }
            }
            .expireWhen { !editor.isValid }
            .withDocumentsCommitted(project)
            .finishOnUiThread(ModalityState.current()) {
                if (it != null) {
                    DocRenderPassFactory.applyItemsToRender(editor, project, it, false)
                } else {
                    DocTranslationService.setInlayDocTranslated(docComment, !value)
                    Toggleable.setSelected(event.presentation, !value)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
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

    companion object {

        private val LOG = Logger.getInstance(TranslateRenderedDocAction::class.java)

        private val Editor.isValid: Boolean
            get() {
                val editorProject = project
                return editorProject != null
                        && !editorProject.isDisposed
                        && !isDisposed
                        && contentComponent.isShowing
            }
    }
}
