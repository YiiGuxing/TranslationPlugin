package cn.yiiguxing.plugin.translate.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.platform.backend.documentation.ContentUpdater
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.util.AsyncSupplier
import kotlin.reflect.KClass


private const val CLASS_RESOLVED_TARGET = "com.intellij.platform.backend.documentation.ResolvedTarget"
private const val CLASS_ASYNC_LINK_RESOLVE_RESULT = "com.intellij.platform.backend.documentation.AsyncLinkResolveResult"
private const val CLASS_ASYNC_RESOLVED_TARGET = "com.intellij.platform.backend.documentation.AsyncResolvedTarget"

private val LOG = logger<TranslatableDocumentationLinkHandler>()


class TranslatableDocumentationLinkHandler : DocumentationLinkHandler {

    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (target !is TranslatableDocumentationTarget) {
            return null
        }

        val originalTarget = target.delegate
        return resolve {
            val resolved = resolveLink(originalTarget, url) ?: return@resolve null
            val kClass = resolved::class
            try {
                when (kClass.qualifiedName) {
                    CLASS_RESOLVED_TARGET -> handleResolvedTarget(target, resolved, kClass)
                    CLASS_ASYNC_LINK_RESOLVE_RESULT -> handleAsyncLinkResolveResult(target, resolved, kClass)
                    else -> resolved
                }
            } catch (_: Throwable) {
                // Handle any exceptions that may occur during resolution
                resolved
            }
        }
    }

    @Suppress("UnstableApiUsage")
    override fun contentUpdater(target: DocumentationTarget, url: String): ContentUpdater? {
        if (target !is TranslatableDocumentationTarget) {
            return null
        }

        val originalTarget = target.delegate
        return resolve { contentUpdater(originalTarget, url) }
    }
}


private inline fun <T> resolve(block: DocumentationLinkHandler.() -> T?): T? {
    @Suppress("UnstableApiUsage")
    for (handler in DocumentationLinkHandler.EP_NAME.extensionList) {
        ProgressManager.checkCanceled()
        if (handler is TranslatableDocumentationLinkHandler) {
            // Skip self to avoid infinite recursion
            continue
        }

        return handler.block() ?: continue
    }

    return null
}

private fun createTranslatableDocumentationTarget(
    originTarget: TranslatableDocumentationTarget,
    target: DocumentationTarget
): TranslatableDocumentationTarget {
    return TranslatableDocumentationTarget(
        project = originTarget.project,
        language = originTarget.language,
        delegate = target
    )
}

private fun handleResolvedTarget(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult,
    kClass: KClass<*>
): LinkResolveResult? {
    val targetProp = kClass.members.firstOrNull { it.name == "target" }
    val target = targetProp?.call(resolved) as? DocumentationTarget ?: return resolved

    return LinkResolveResult.resolvedTarget(createTranslatableDocumentationTarget(originTarget, target))
}

private fun handleAsyncLinkResolveResult(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult,
    kClass: KClass<*>
): LinkResolveResult? {
    return LinkResolveResult.asyncResult {
        val supplierProp = kClass.members.firstOrNull { it.name == "supplier" }
        if (supplierProp == null) {
            LOG.warn("AsyncLinkResolveResult supplier property not found")
            return@asyncResult null
        }

        val supplier = supplierProp.call(resolved) as? AsyncSupplier<*>
        if (supplier == null) {
            LOG.warn(
                "AsyncLinkResolveResult supplier expected to be AsyncSupplier, but was ${supplierProp.returnType}"
            )
            return@asyncResult null
        }

        val asyncResolvedTarget = supplier.invoke()
        if (asyncResolvedTarget == null) {
            return@asyncResult null
        }
        if (asyncResolvedTarget !is LinkResolveResult.Async) {
            val className = asyncResolvedTarget::class.qualifiedName
            LOG.warn("Async supplier did not return an LinkResolveResult.Async: $className")
            return@asyncResult null
        }

        val clazz = asyncResolvedTarget::class
        when (clazz.qualifiedName) {
            CLASS_ASYNC_RESOLVED_TARGET -> handleAsyncResolvedTarget(
                originTarget,
                asyncResolvedTarget,
                clazz
            )

            else -> asyncResolvedTarget
        }
    }
}

private suspend fun handleAsyncResolvedTarget(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult.Async,
    kClass: KClass<*>
): LinkResolveResult.Async? {
    val pointerProp = kClass.members.firstOrNull { it.name == "pointer" }

    @Suppress("UnstableApiUsage")
    val pointer = pointerProp?.call(resolved) as? Pointer<*> ?: return resolved

    return readAction {
        @Suppress("UnstableApiUsage")
        val target = pointer.dereference() as? DocumentationTarget
            ?: return@readAction resolved

        LinkResolveResult.Async.resolvedTarget(createTranslatableDocumentationTarget(originTarget, target))
    }
}
