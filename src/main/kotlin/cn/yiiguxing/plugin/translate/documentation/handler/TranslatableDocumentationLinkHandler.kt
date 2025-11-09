package cn.yiiguxing.plugin.translate.documentation.handler

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.model.Pointer
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.platform.backend.documentation.ContentUpdater
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.util.AsyncSupplier
import kotlin.reflect.KCallable
import kotlin.reflect.full.declaredMemberProperties


private const val CLASS_RESOLVED_TARGET = "com.intellij.platform.backend.documentation.ResolvedTarget"
private const val CLASS_ASYNC_LINK_RESOLVE_RESULT = "com.intellij.platform.backend.documentation.AsyncLinkResolveResult"
private const val CLASS_ASYNC_RESOLVED_TARGET = "com.intellij.platform.backend.documentation.AsyncResolvedTarget"

private val LOG = logger<TranslatableDocumentationLinkHandler>()


class TranslatableDocumentationLinkHandler : DocumentationLinkHandler {

    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (target !is TranslatableDocumentationTarget) {
            return null
        }

        val originalTarget = target.wrapped
        return resolve {
            val resolved = resolveLink(originalTarget, url) ?: return@resolve null
            try {
                when (resolved::class.qualifiedName) {
                    CLASS_RESOLVED_TARGET -> handleResolvedTarget(target, resolved)
                    CLASS_ASYNC_LINK_RESOLVE_RESULT -> handleAsyncLinkResolveResult(target, resolved)
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

        val originalTarget = target.wrapped
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
        wrapped = target
    )
}


private val TARGET_PROPERTY = PropertyGetter("target")

private fun handleResolvedTarget(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult
): LinkResolveResult {
    val target: DocumentationTarget = TARGET_PROPERTY.getAs(resolved) ?: return resolved
    return LinkResolveResult.resolvedTarget(createTranslatableDocumentationTarget(originTarget, target))
}


private val SUPPLIER_PROPERTY = PropertyGetter("supplier")

private fun handleAsyncLinkResolveResult(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult
): LinkResolveResult {
    return LinkResolveResult.asyncResult {
        val supplier: AsyncSupplier<*> = SUPPLIER_PROPERTY.getAs(resolved) ?: return@asyncResult null
        val asyncResolvedTarget = supplier.invoke() ?: return@asyncResult null
        if (asyncResolvedTarget !is LinkResolveResult.Async) {
            val className = asyncResolvedTarget::class.qualifiedName
            LOG.warn("Async supplier did not return an LinkResolveResult.Async: $className")
            return@asyncResult null
        }

        when (asyncResolvedTarget::class.qualifiedName) {
            CLASS_ASYNC_RESOLVED_TARGET -> handleAsyncResolvedTarget(
                originTarget,
                asyncResolvedTarget,
            )

            else -> asyncResolvedTarget
        }
    }
}


private var POINTER_PROPERTY = PropertyGetter("pointer")

private suspend fun handleAsyncResolvedTarget(
    originTarget: TranslatableDocumentationTarget,
    resolved: LinkResolveResult.Async
): LinkResolveResult.Async {
    @Suppress("UnstableApiUsage")
    val pointer: Pointer<*> = POINTER_PROPERTY.getAs(resolved) ?: return resolved
    return readAction {
        @Suppress("UnstableApiUsage")
        val target = pointer.dereference() as? DocumentationTarget
            ?: return@readAction resolved

        LinkResolveResult.Async.resolvedTarget(createTranslatableDocumentationTarget(originTarget, target))
    }
}


private class PropertyGetter(private val name: String) {

    @Volatile
    private var property: KCallable<*>? = null

    @Volatile
    private var isInitialized = false

    fun get(receiver: Any): Any? {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    property = receiver::class.declaredMemberProperties.firstOrNull { it.name == name }.also {
                        if (it == null) {
                            LOG.warn("Property '$name' not found in ${receiver::class.qualifiedName}")
                        }
                    }
                    isInitialized = true
                }
            }
        }

        return property?.let {
            try {
                it.call(receiver)
            } catch (_: Throwable) {
                LOG.warn("Failed to get property '$name' from ${receiver::class.qualifiedName}")
                null
            }
        }
    }

    inline fun <reified T> getAs(receiver: Any): T? {
        val result = get(receiver)
        if (result is T) {
            return result
        }

        LOG.warn(
            "Property '$name' is not of type ${T::class.qualifiedName} in ${
                receiver::class.qualifiedName
            }, expected ${result?.javaClass?.name ?: "null"}"
        )

        return null
    }
}
