package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.util.findElementOfTypeAt
import com.intellij.openapi.util.Key
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Nls

private val TRANSLATION_INFO_KEY = Key.create<InlineDocTranslationInfo>("translation.inlineDocumentation.info")


/**
 * Sets the inline documentation translation [info] for the given PSI [comment].
 */
internal fun setPsiInlineDocumentationTranslationInfo(
    comment: PsiDocCommentBase,
    info: InlineDocTranslationInfo?
) {
    comment.putUserData(TRANSLATION_INFO_KEY, info)
}

/**
 * Returns the inline documentation translation info for the given PSI [comment].
 */
internal fun getPsiInlineDocumentationTranslationInfo(comment: PsiDocCommentBase): InlineDocTranslationInfo? {
    return comment.getUserData(TRANSLATION_INFO_KEY)
}

internal data class InlineDocTranslationInfo(
    val translatedText: String? = null,
    val translatorId: String? = null,
    val isLoading: Boolean = false,
    val isDisabled: Boolean = false,
    val hasError: Boolean = false,
) {

    fun disabled(disabled: Boolean): InlineDocTranslationInfo {
        check(!isLoading && !hasError && translatedText != null) {
            "Cannot disable translation info when it is loading, has error, or not translated."
        }
        return copy(isDisabled = disabled)
    }

    companion object {
        fun loading(): InlineDocTranslationInfo = InlineDocTranslationInfo(isLoading = true)

        fun translated(
            translatedText: String,
            translatorId: String,
            hasError: Boolean = false
        ): InlineDocTranslationInfo {
            return InlineDocTranslationInfo(
                translatedText = translatedText,
                translatorId = translatorId,
                hasError = hasError
            )
        }
    }
}

@Suppress("UnstableApiUsage")
internal class TranslatableInlineDocumentation(
    private val file: PsiFile,
    private val delegate: InlineDocumentation
) : InlineDocumentation by delegate {

    override fun renderText(): @Nls String? {
        val renderText = delegate.renderText() ?: return null

        @Suppress("OverrideOnly")
        val offset = documentationRange.startOffset
        val comment = file.findElementOfTypeAt<PsiDocCommentBase>(offset) ?: return renderText

        return getPsiInlineDocumentationTranslationInfo(comment)?.let { info ->
            info.translatedText?.takeIf { !info.isDisabled }
        } ?: renderText
    }
}
