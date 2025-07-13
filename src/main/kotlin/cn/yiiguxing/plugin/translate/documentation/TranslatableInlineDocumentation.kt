package cn.yiiguxing.plugin.translate.documentation

import com.intellij.openapi.util.Key
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls


@Suppress("UnstableApiUsage")
internal class TranslatableInlineDocumentation(
    private val file: PsiFile,
    private val delegate: InlineDocumentation
) : InlineDocumentation by delegate {

    override fun renderText(): @Nls String? {
        val renderText = delegate.renderText() ?: return null

        @Suppress("OverrideOnly")
        val comment = PsiTreeUtil.getParentOfType(
            file.findElementAt(documentationRange.startOffset),
            PsiDocCommentBase::class.java,
            false
        )
        if (comment != null && comment.getUserData(TRANSLATION_FLAG_KEY) == true) {
            // If the translation flag is set, return the original text
            return "<html translated><body>renderText</body></html>"
        }

        return renderText
    }

    companion object {
        val TRANSLATION_FLAG_KEY = Key.create<Boolean>("translation.inlineDocumentation.translationFlag")
    }
}