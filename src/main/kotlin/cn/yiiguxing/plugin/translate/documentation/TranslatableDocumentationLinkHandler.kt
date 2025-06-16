package cn.yiiguxing.plugin.translate.documentation

import com.intellij.openapi.progress.ProgressManager
import com.intellij.platform.backend.documentation.ContentUpdater
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult


class TranslatableDocumentationLinkHandler : DocumentationLinkHandler {

    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (target !is TranslatableDocumentationTarget) {
            return null
        }

        val originalTarget = target.delegate
        return resolve { resolveLink(originalTarget, url) }
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
