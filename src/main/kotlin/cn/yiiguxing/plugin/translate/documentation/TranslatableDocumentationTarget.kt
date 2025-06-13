package cn.yiiguxing.plugin.translate.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget

/**
 * A [DocumentationTarget] that supports translation.
 */
@Suppress("UnstableApiUsage")
class TranslatableDocumentationTarget(
    private val delegate: DocumentationTarget,
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
        // TODO: Implement translation logic here
        return delegate.computeDocumentation()
    }

}