package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.SKIP_WHITE_SPACE
import cn.yiiguxing.plugin.translate.util.elementType
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.lexer.CSharpTokenType
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.CSharpDummyNode
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.psi.impl.CSharpDummyDeclaration

/**
 * 由于`C#`的[DocumentationProvider]的机制与常规的不同，并隐藏了文档生成的细节，通过
 * `C#`的[DocumentationProvider]并不能正确地获取到生成的文档，因此该类并不能实现最终
 * 的目的。
 * 另见：`com.jetbrains.rdclient.quickDoc.FrontendDocumentationProvider`
 */
class CSharpDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        // 2019版本之前没有C#的PSI
        if (!IdeVersion.isIde2019OrNewer) {
            return null
        }

        val element = psiFile.findElementAt(offset)
        return (element as? PsiComment)?.takeIf { it.owner != null }
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return (documentationElement as? PsiComment)?.owner
    }

    private companion object {
        val SKIP_WHITE_SPACE_AND_COMMENT: (PsiElement) -> Boolean = { it is PsiWhiteSpace || it is PsiComment }

        val PsiComment.isDocComment: Boolean
            get() = when (elementType) {
                CSharpTokenType.END_OF_LINE_COMMENT -> text.startsWith("///")
                CSharpTokenType.C_STYLE_COMMENT -> text.startsWith("/**")
                else -> false
            }

        val PsiComment.owner: PsiElement?
            get() {
                if (!isDocComment) {
                    return null
                }

                val element = getNextSiblingSkippingCondition(SKIP_WHITE_SPACE_AND_COMMENT) as? CSharpDummyNode
                    ?: return null
                val nextNode = element.getNextSiblingSkippingCondition(SKIP_WHITE_SPACE)
                if (nextNode is CSharpDummyDeclaration) {
                    return nextNode.declaredElement
                }

                return element.identifier
            }

        val CSharpDummyNode.identifier: PsiElement?
            get() {
                var brackets = 0
                var element = firstChild
                while (element != null) {
                    when (element.elementType) {
                        CSharpTokenType.LBRACKET, CSharpTokenType.LPARENTH -> brackets++
                        CSharpTokenType.RBRACKET, CSharpTokenType.RPARENTH -> brackets--
                        CSharpTokenType.IDENTIFIER -> {
                            if (brackets == 0) {
                                return element
                            }
                        }
                    }

                    element = element.nextSibling
                }

                return null
            }
    }

}