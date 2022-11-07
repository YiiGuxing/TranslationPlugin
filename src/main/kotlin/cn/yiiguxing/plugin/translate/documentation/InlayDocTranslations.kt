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
internal class InlayDocTranslations : Disposable {

    //use SmartPsiElementPointer to survive reparse
    private val translationResults: MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, TranslationResult> =
        ConcurrentHashMap()

    override fun dispose() {
        translationResults.clear()
    }

    companion object {
        private val EMPTY = TranslationResult()

        private fun service(project: Project) = project.getService(InlayDocTranslations::class.java)

        private fun translationResults(project: Project) = service(project).translationResults

        /**
         * Returns `true` if the specified [doc comment element][docComment] is in translated status.
         */
        fun isTranslated(docComment: PsiDocCommentBase): Boolean {
            return SmartPointerManager.createPointer(docComment) in translationResults(docComment.project)
        }

        /**
         * Sets the translation status of the specified [doc comment element][docComment].
         */
        fun setTranslated(docComment: PsiDocCommentBase, value: Boolean) {
            val translationResults = translationResults(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)

            if (value) {
                translationResults[pointer] = translationResults[pointer] ?: EMPTY
            } else {
                translationResults.remove(pointer)
            }

            translationResults.scheduleCleanup()
        }

        /**
         * Returns the [TranslationResult] of the specified [doc comment element][docComment],
         * or `null` if the element is not in the translated status,
         * or an empty [TranslationResult] if the translation is not ready.
         */
        fun getTranslationResult(docComment: PsiDocCommentBase): TranslationResult? {
            val translationResults = translationResults(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)
            return translationResults[pointer]
        }

        /**
         * Update the [translation result][translationResult] of the specified [doc comment element][docComment],
         * clear the translation status of the element if the [translation result][translationResult] is `null`.
         */
        fun updateTranslationResult(docComment: PsiDocCommentBase, translationResult: TranslationResult?) {
            val translationResults = translationResults(docComment.project)
            val pointer = SmartPointerManager.createPointer(docComment)
            if (translationResult != null) {
                translationResults[pointer] = translationResult
            } else {
                translationResults.remove(pointer)
            }

            translationResults.scheduleCleanup()
        }

        private fun MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, *>.scheduleCleanup() {
            ReadAction.nonBlocking {
                keys.removeIf { it.element == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    data class TranslationResult(val original: String? = null, val translation: String? = null)

}