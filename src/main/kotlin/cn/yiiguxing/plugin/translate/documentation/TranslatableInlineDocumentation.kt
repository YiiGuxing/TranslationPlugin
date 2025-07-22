package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget.Companion.TRANSLATION_FLAG_KEY
import com.intellij.openapi.util.Key
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls

private val LOCAL_TRANSLATION_STATE = ThreadLocal.withInitial { false }
private val TRANSLATION_STATE_KEY = Key.create<Boolean>("translation.inlineDocumentation.translationState")

/**
 * Sets the translation state of the inline documentations for the current thread.
 */
fun setInlineDocumentationLocalTranslationEnabled(enabled: Boolean) {
    LOCAL_TRANSLATION_STATE.set(enabled)
}

/**
 * Sets the translation state of the inline documentation for the given [comment][PsiDocCommentBase].
 */
internal fun setPsiInlineDocumentationTranslationEnabled(comment: PsiDocCommentBase, enabled: Boolean) {
    comment.putUserData(TRANSLATION_FLAG_KEY, enabled)
}

/**
 * Gets the translation state of the inline documentation for the given [comment][PsiDocCommentBase].
 *
 * @return `true` if translation is enabled, `false` if disabled, or `null` if not set.
 */
internal fun isPsiInlineDocumentationTranslationEnabled(comment: PsiDocCommentBase): Boolean? {
    return comment.getUserData(TRANSLATION_FLAG_KEY)
}

@Suppress("UnstableApiUsage")
internal class TranslatableInlineDocumentation(
    private val file: PsiFile,
    private val delegate: InlineDocumentation
) : InlineDocumentation by delegate {

    private fun isTranslationEnabled(): Boolean {
        if (LOCAL_TRANSLATION_STATE.get()) {
            // If local translation is enabled, return true
            return true
        }

        @Suppress("OverrideOnly", "UnstableApiUsage")
        val comment = PsiTreeUtil.getParentOfType(
            file.findElementAt(documentationRange.startOffset),
            PsiDocCommentBase::class.java,
            false
        ) ?: return false

        return isPsiInlineDocumentationTranslationEnabled(comment) ?: false
    }

    override fun renderText(): @Nls String? {
        val renderText = delegate.renderText() ?: return null
        if (isTranslationEnabled().not()) {
            // If translation is not enabled, return the original text
            return renderText
        }

        return translateText(renderText)
    }

    private fun translateText(text: String): @Nls String? {
        Thread.sleep(5000)
        return "<html translated><body>Rendered Text.</body></html>"
    }
}
