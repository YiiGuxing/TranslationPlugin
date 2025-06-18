package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.Settings
import com.intellij.model.Pointer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.*
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.createSmartPointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * A [DocumentationTarget] that supports translation.
 */
@Suppress("UnstableApiUsage")
internal class TranslatableDocumentationTarget private constructor(
    val delegate: DocumentationTarget,
    private val pointer: TranslatableDocumentationTargetPointer
) : DocumentationTarget by delegate {

    /**
     * Indicates whether the documentation target should be translated.
     */
    var translate: Boolean
        get() = pointer.translate
        set(value) {
            pointer.translate = value
        }

    init {
        check(delegate !is TranslatableDocumentationTarget) {
            "TranslatableDocumentationTarget should not be nested: $delegate"
        }
    }

    constructor(
        project: Project,
        delegate: DocumentationTarget,
        translate: Boolean = service<Settings>().translateDocumentation
    ) : this(
        delegate,
        DefaultTranslatableDocumentationTargetPointer(
            project,
            createPointer(delegate),
            translate
        )
    )

    constructor(
        project: Project,
        delegate: DocumentationTarget,
        psiElement: PsiElement
    ) : this(
        delegate,
        TranslatablePsiDocumentationTargetPointer(
            project,
            createPointer(delegate),
            psiElement.createSmartPointer()
        )
    )

    override fun createPointer(): Pointer<out DocumentationTarget> = pointer

    override fun computeDocumentation(): DocumentationResult? {
        val originalResult = delegate.computeDocumentation()
        if (!translate ||
            originalResult == null ||
            !(originalResult is AsyncDocumentation || originalResult is DocumentationData)
        ) {
            return originalResult
        }

        return DocumentationResult.asyncDocumentation {
            val documentation = when (originalResult) {
                is AsyncDocumentation -> originalResult.supplier()
                is DocumentationResult.Documentation -> originalResult
            }
            if (documentation !is DocumentationData) {
                return@asyncDocumentation documentation
            }

            val translatedDocumentationFlow = flow {
                val translatedDocumentation = withContext(Dispatchers.IO) {
                    // TODO: 1. Implement translation logic here.
                    //       2. Return the translated version of the documentation.
                    "<html><body>Translated Documentation</body></html>"
                }
                emit(DocumentationContent.content(translatedDocumentation))
            }

            // TODO: Append the translating status to the original documentation
            documentation.updates(translatedDocumentationFlow)
        }
    }


    private sealed interface TranslatableDocumentationTargetPointer : Pointer<TranslatableDocumentationTarget> {
        val project: Project
        var translate: Boolean
    }

    private class DefaultTranslatableDocumentationTargetPointer(
        override val project: Project,
        private val delegatePointer: Pointer<out DocumentationTarget>,
        @Volatile override var translate: Boolean
    ) : TranslatableDocumentationTargetPointer {
        override fun dereference(): TranslatableDocumentationTarget? {
            val target = delegatePointer.dereference() ?: return null
            return TranslatableDocumentationTarget(target, this)
        }
    }

    private class TranslatablePsiDocumentationTargetPointer(
        override val project: Project,
        private val delegatePointer: Pointer<out DocumentationTarget>,
        private val psiElementPointer: Pointer<out PsiElement>
    ) : TranslatableDocumentationTargetPointer {

        private val settings: Settings by lazy { service<Settings>() }

        override var translate: Boolean
            get() = psiElementPointer.dereference()
                ?.let { DocTranslationService.getTranslationState(it) ?: settings.translateDocumentation }
                ?: false
            set(value) {
                psiElementPointer.dereference()?.let {
                    DocTranslationService.setTranslationState(it, value)
                }
            }

        override fun dereference(): TranslatableDocumentationTarget? {
            val target = delegatePointer.dereference() ?: return null
            psiElementPointer.dereference() ?: return null

            return TranslatableDocumentationTarget(target, this)
        }
    }
}


@Suppress("OverrideOnly", "UnstableApiUsage")
private fun createPointer(target: DocumentationTarget): Pointer<out DocumentationTarget> {
    ApplicationManager.getApplication().assertReadAccessAllowed()
    return target.createPointer()
}