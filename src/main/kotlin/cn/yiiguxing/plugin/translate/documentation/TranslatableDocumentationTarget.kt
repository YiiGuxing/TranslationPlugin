package cn.yiiguxing.plugin.translate.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.AsyncDocumentation
import com.intellij.platform.backend.documentation.DocumentationContent
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * A [DocumentationTarget] that supports translation.
 */
@Suppress("UnstableApiUsage")
class TranslatableDocumentationTarget(
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
            TranslatableDocumentationTarget(target, shouldTranslate)
        }
    }

    override fun computeDocumentation(): DocumentationResult? {
        val originalResult = delegate.computeDocumentation()
        if (!shouldTranslate || originalResult == null) {
            return originalResult
        }

        return DocumentationResult.asyncDocumentation {
            val documentation = when (originalResult) {
                is AsyncDocumentation -> originalResult.supplier()
                else -> originalResult as DocumentationResult.Documentation
            } ?: return@asyncDocumentation null

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