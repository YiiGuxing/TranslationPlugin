package cn.yiiguxing.plugin.translate.documentation

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ConcurrentHashMap

@Service
internal class TranslatedInlayDocumentations : Disposable {

    //use SmartPsiElementPointer to survive reparse
    private val translatedDocs: MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, TranslatedDoc> =
        ConcurrentHashMap()

    override fun dispose() {
        translatedDocs.clear()
    }

    companion object {
        private val EMPTY = TranslatedDoc()

        private fun service(project: Project) = project.getService(TranslatedInlayDocumentations::class.java)

        private fun translatedDocs(project: Project) = service(project).translatedDocs

        /**
         * Returns `true` if the specified [doc comment][docComment] is in translated status.
         */
        fun isTranslated(docComment: PsiDocCommentBase): Boolean {
            return SmartPointerManager.createPointer(docComment) in translatedDocs(docComment.project)
        }

        /**
         * Sets the translation status of the specified [doc comment][docComment].
         */
        fun setTranslated(docComment: PsiDocCommentBase, value: Boolean) {
            val translatedDocs = translatedDocs(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)

            if (value) {
                translatedDocs[pointer] = translatedDocs[pointer] ?: EMPTY
            } else {
                translatedDocs.remove(pointer)
            }

            translatedDocs.scheduleCleanup()
        }

        /**
         * Returns the [TranslatedDoc] of the specified [doc comment][docComment],
         * or `null` if the comment is not in the translated status,
         * or an empty [TranslatedDoc] if the translation is not ready.
         */
        fun getTranslatedDoc(docComment: PsiDocCommentBase): TranslatedDoc? {
            val translatedDocs = translatedDocs(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)
            return translatedDocs[pointer]
        }

        /**
         * Update the [translatedDoc] of the specified [doc comment][docComment],
         * clear the translation status of the comment if the [translatedDoc] is `null`.
         */
        fun updateTranslatedDoc(docComment: PsiDocCommentBase, translatedDoc: TranslatedDoc?) {
            val translatedDocs = translatedDocs(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)
            if (translatedDoc != null) {
                translatedDocs[pointer] = translatedDoc
            } else {
                translatedDocs.remove(pointer)
            }

            translatedDocs.scheduleCleanup()
        }

        private fun MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, *>.scheduleCleanup() {
            ReadAction.nonBlocking {
                keys.removeIf { it.element == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    data class TranslatedDoc(val original: String? = null, val translation: String? = null)

}