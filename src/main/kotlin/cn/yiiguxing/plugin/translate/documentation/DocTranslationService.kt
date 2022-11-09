package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.util.LruCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ConcurrentHashMap

@Service
internal class DocTranslationService : Disposable {

    // Use SmartPsiElementPointer to survive reparse.
    private val translationStates: LruCache<SmartPsiElementPointer<PsiElement>, Boolean> = LruCache(MAX_STATES_SIZE)

    private val inlayDocTranslations: MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, TranslationResult> =
        ConcurrentHashMap()


    override fun dispose() {
        translationStates.evictAll()
        inlayDocTranslations.clear()
    }


    companion object {

        private const val MAX_STATES_SIZE = 1024

        private val EMPTY = TranslationResult()

        private val <T : PsiElement> T.myPointer: SmartPsiElementPointer<T>
            get() = SmartPointerManager.createPointer(this)

        private fun service(project: Project) = project.getService(DocTranslationService::class.java)

        private fun translationStates(project: Project) = service(project).translationStates

        private fun inlayDocTranslations(project: Project) = service(project).inlayDocTranslations


        /**
         * Sets the [translation state][translationState] of the specified [PSI element][element].
         */
        fun setTranslationState(element: PsiElement, translationState: Boolean) {
            translationStates(element.project).let { translationStates ->
                translationStates.put(element.myPointer, translationState)
                translationStates.scheduleCleanup()
            }
        }

        /**
         * Returns the translation state of the specified [PSI element][element],
         * returns `null` if the translation state is not set.
         */
        fun getTranslationState(element: PsiElement): Boolean? {
            return translationStates(element.project).let { translationStates ->
                translationStates[element.myPointer].also {
                    translationStates.scheduleCleanup()
                }
            }
        }

        /**
         * Returns `true` if the specified [doc comment element][docComment] is in translated status.
         */
        fun isInlayDocTranslated(docComment: PsiDocCommentBase): Boolean {
            return docComment.myPointer in inlayDocTranslations(docComment.project)
        }

        /**
         * Sets the translation status of the specified [doc comment element][docComment].
         */
        fun setInlayDocTranslated(docComment: PsiDocCommentBase, value: Boolean) {
            val inlayDocTranslations = inlayDocTranslations(docComment.project)
            val pointer = docComment.myPointer

            if (value) {
                inlayDocTranslations[pointer] = inlayDocTranslations[pointer] ?: EMPTY
            } else {
                inlayDocTranslations.remove(pointer)
            }

            inlayDocTranslations.scheduleCleanup()
        }

        /**
         * Returns the [TranslationResult] of the specified [doc comment element][docComment],
         * or `null` if the element is not in the translated status,
         * or an empty [TranslationResult] if the translation is not ready.
         */
        fun getInlayDocTranslation(docComment: PsiDocCommentBase): TranslationResult? {
            return inlayDocTranslations(docComment.project)[docComment.myPointer]
        }

        /**
         * Update the [translation result][translationResult] of the specified [doc comment element][docComment],
         * clear the translation status of the element if the [translation result][translationResult] is `null`.
         */
        fun updateInlayDocTranslation(docComment: PsiDocCommentBase, translationResult: TranslationResult?) {
            val inlayDocTranslations = inlayDocTranslations(docComment.project)
            val pointer = docComment.myPointer
            if (translationResult != null) {
                inlayDocTranslations[pointer] = translationResult
            } else {
                inlayDocTranslations.remove(pointer)
            }

            inlayDocTranslations.scheduleCleanup()
        }

        private fun LruCache<SmartPsiElementPointer<PsiElement>, *>.scheduleCleanup() {
            ReadAction.nonBlocking {
                removeIf { key, _ -> key.element == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }

        private fun MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, *>.scheduleCleanup() {
            ReadAction.nonBlocking {
                keys.removeIf { it.element == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    data class TranslationResult(val original: String? = null, val translation: String? = null)

}