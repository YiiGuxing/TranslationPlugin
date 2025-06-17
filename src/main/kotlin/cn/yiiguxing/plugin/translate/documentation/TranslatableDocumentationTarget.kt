package cn.yiiguxing.plugin.translate.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [DocumentationTarget] that supports translation.
 * The [translate] state will be shared among all instances created via the [createPointer] method.
 */
@Suppress("UnstableApiUsage")
internal class TranslatableDocumentationTarget private constructor(
    private val project: Project,
    val delegate: DocumentationTarget,
    private val translateRef: AtomicBoolean
) : DocumentationTarget by delegate {

    /**
     * Indicates whether the documentation target should be translated.
     *
     * **Note**: This property is shared among all instances created via the [createPointer] method.
     */
    var translate: Boolean
        get() = translateRef.get()
        set(value) {
            translateRef.set(value)
        }

    init {
        check(delegate !is TranslatableDocumentationTarget) {
            "TranslatableDocumentationTarget should not be nested: $delegate"
        }
    }

    constructor(project: Project, delegate: DocumentationTarget, translate: Boolean = false) : this(
        project, delegate, AtomicBoolean(translate)
    )

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val originalPointer = delegate.createPointer()
        return Pointer {
            val target = originalPointer.dereference() ?: return@Pointer null
            // Passing `translateRef` to ensure the state is preserved and shared cross instances.
            TranslatableDocumentationTarget(project, target, translateRef)
        }
    }

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
}