package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.findElementOfTypeAt
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface DocumentationElementProvider {

    /**
     * Finds a documentation PSI element at the specified [offset] from the specified [PSI file][psiFile].
     */
    fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement?

    /**
     * Returns the owner of the specified [documentationElement].
     */
    fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return if (documentationElement is PsiDocCommentBase) {
            documentationElement.owner
        } else {
            documentationElement.parent
        }
    }

    private object DefaultDocumentationElementProvider : DocumentationElementProvider {

        /**
         * 预先加载类，以避免在运行时加载。类的加载消耗大，运行时加载会导致阻塞UI线程？
         * 相关ISSUE：[#2183](https://github.com/YiiGuxing/TranslationPlugin/issues/2183)
         */
        @JvmStatic
        private val TARGET_TYPE: Class<PsiDocCommentBase> = PsiDocCommentBase::class.java

        override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
            return psiFile.findElementOfTypeAt(offset, TARGET_TYPE)
                ?.takeIf { it.owner != null }
        }

    }

    companion object {
        private val PROVIDERS = LanguageExtension<DocumentationElementProvider>(
            "${TranslationPlugin.PLUGIN_ID}.docElementProvider",
            DefaultDocumentationElementProvider
        )

        fun forLanguage(language: Language): DocumentationElementProvider = PROVIDERS.forLanguage(language)
    }

}
