package cn.yiiguxing.plugin.translate.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * A [DocumentationTarget] that supports translation.
 */
@Suppress("UnstableApiUsage")
class TranslatableDocumentationTarget(
    private val project: Project,
    internal val delegate: DocumentationTarget,
    @Volatile var shouldTranslate: Boolean = false
) : DocumentationTarget by delegate {

    init {
        check(delegate !is TranslatableDocumentationTarget) {
            "TranslatableDocumentationTarget should not be nested: $delegate"
        }
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val originalPointer = delegate.createPointer()
        return Pointer {
            val target = originalPointer.dereference() ?: return@Pointer null
            TranslatableDocumentationTarget(project, target, shouldTranslate)
        }
    }

    override fun computeDocumentation(): DocumentationResult? {
        val originalResult = delegate.computeDocumentation()
        if (!shouldTranslate ||
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