package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.provider.TranslatedDocumentationProvider
import cn.yiiguxing.plugin.translate.util.LruCache
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
internal class DocTranslationService : Disposable {

    private val lastCleanupTime: AtomicLong = AtomicLong(System.currentTimeMillis())

    // Use SmartPsiElementPointer to survive reparse.
    private val translationStates: LruCache<SmartPsiElementPointer<PsiElement>, Boolean> = LruCache(MAX_STATES_SIZE)

    private val inlayDocTranslations: MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, TranslationResult> =
        ConcurrentHashMap()


    private fun scheduleCleanup() {
        val now = System.currentTimeMillis()
        val updated = now != lastCleanupTime.get() && lastCleanupTime.accumulateAndGet(now) { l, n ->
            if (n - l >= CLEANUP_INTERVAL) n else l
        } == now
        if (updated) {
            ReadAction.nonBlocking {
                translationStates.removeIf { key, _ -> key.elementOrNull == null }
                inlayDocTranslations.keys.removeIf { it.elementOrNull == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    override fun dispose() {
        translationStates.evictAll()
        inlayDocTranslations.clear()
    }


    companion object {

        private const val MAX_STATES_SIZE = 1024
        private const val CLEANUP_INTERVAL = 60 * 1000L // one minute.

        private val EMPTY = TranslationResult()

        private val LOG = logger<DocTranslationService>()

        private val <T : PsiElement> T.myPointer: SmartPsiElementPointer<T>
            get() = SmartPointerManager.createPointer(this)

        private val <T : PsiElement> SmartPsiElementPointer<T>.elementOrNull: T?
            get() = try {
                element
            } catch (e: Throwable) {
                LOG.w("Cannot get element from this smart pointer", e)
                null
            }

        private fun service(project: Project) = project.getService(DocTranslationService::class.java)

        private inline fun <T> withTranslationStates(
            project: Project,
            block: (LruCache<SmartPsiElementPointer<PsiElement>, Boolean>) -> T
        ): T {
            val service = service(project)
            val result = block(service.translationStates)
            service.scheduleCleanup()
            return result
        }

        private inline fun <T> withInlayDocTranslations(
            project: Project,
            block: (MutableMap<SmartPsiElementPointer<PsiDocCommentBase>, TranslationResult>) -> T
        ): T {
            val service = service(project)
            val result = block(service.inlayDocTranslations)
            service.scheduleCleanup()
            return result
        }


        /**
         * Sets the [translation state][translationState] of the specified [PSI element][element].
         */
        fun setTranslationState(element: PsiElement, translationState: Boolean) {
            if (!element.isValid) {
                return
            }
            withTranslationStates(element.project) { translationStates ->
                translationStates.put(element.myPointer, translationState)
            }
        }

        /**
         * Returns the translation state of the specified [PSI element][element],
         * returns `null` if the translation state is not set.
         */
        fun getTranslationState(element: PsiElement): Boolean? {
            if (!element.isValid) {
                return null
            }

            return withTranslationStates(element.project) { translationStates ->
                translationStates[element.myPointer]
            }
        }

        /**
         * Returns `true` if the specified [doc comment element][docComment] is in translated status.
         */
        fun isInlayDocTranslated(docComment: PsiDocCommentBase): Boolean {
            return withInlayDocTranslations(docComment.project) { inlayDocTranslations ->
                docComment.myPointer in inlayDocTranslations
            }
        }

        /**
         * Sets the translation status of the specified [doc comment element][docComment].
         */
        fun setInlayDocTranslated(docComment: PsiDocCommentBase, value: Boolean) {
            withInlayDocTranslations(docComment.project) { inlayDocTranslations ->
                val pointer = docComment.myPointer

                if (value) {
                    inlayDocTranslations[pointer] = inlayDocTranslations[pointer] ?: EMPTY
                } else {
                    inlayDocTranslations.remove(pointer)
                }
            }
        }

        /**
         * Returns the [TranslationResult] of the specified [doc comment element][docComment],
         * or `null` if the element is not in the translated status,
         * or an empty [TranslationResult] if the translation is not ready.
         */
        fun getInlayDocTranslation(docComment: PsiDocCommentBase): TranslationResult? {
            return withInlayDocTranslations(docComment.project) { inlayDocTranslations ->
                inlayDocTranslations[docComment.myPointer]
            }
        }

        /**
         * Update the [translation result][translationResult] of the specified [doc comment element][docComment],
         * clear the translation status of the element if the [translation result][translationResult] is `null`.
         */
        fun updateInlayDocTranslation(docComment: PsiDocCommentBase, translationResult: TranslationResult?) {
            withInlayDocTranslations(docComment.project) { inlayDocTranslations ->
                val pointer = docComment.myPointer
                if (translationResult != null) {
                    inlayDocTranslations[pointer] = translationResult
                } else {
                    inlayDocTranslations.remove(pointer)
                }
            }
        }

        /**
         * Returns `true` if the specified [PSI element][element] supports
         * documentation translation, otherwise returns `false`.
         */
        fun isSupportedForPsiElement(element: PsiElement): Boolean {
            if (!element.isValid) {
                return false
            }

            val originalElement = DocumentationManager.getOriginalElement(element)
            val provider = try {
                ReadAction.compute<DocumentationProvider, Throwable> {
                    DocumentationManager.getProviderFromElement(element, originalElement)
                }
            } catch (e: Throwable) {
                LOG.w("Cannot get documentation provider from element", e)
                return false
            }

            return includesTranslatedDocumentationProvider(provider)
        }

        /**
         * Returns `true` if the specified [provider] or any of
         * its sub-providers is a [TranslatedDocumentationProvider].
         */
        private fun includesTranslatedDocumentationProvider(provider: DocumentationProvider): Boolean {
            return when (provider) {
                is TranslatedDocumentationProvider -> true
                is CompositeDocumentationProvider -> provider.providers.any { includesTranslatedDocumentationProvider(it) }
                else -> false
            }
        }
    }

    data class TranslationResult(val original: String? = null, val translation: String? = null)

}