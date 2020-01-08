package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.findChildOfType
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import com.goide.psi.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace

class GoDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val element = psiFile.findElementAt(offset)
        return (element as? PsiComment)?.takeIf { it.owner != null }
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return (documentationElement as? PsiComment)?.owner
    }

    private companion object {
        val SKIP_WHITE_SPACE_AND_COMMENT: (PsiElement) -> Boolean = {
            (it is PsiWhiteSpace && it.text.count { char -> char == '\n' } <= 1) || it is PsiComment
        }

        val GoTypeDeclaration.innerOwner: PsiElement?
            get() = findChildOfType(GoTypeSpec::class.java)

        val GoVarDeclaration.innerOwner: PsiElement?
            get() = findChildOfType(GoVarDefinition::class.java, true)

        val PsiComment.owner: PsiElement?
            get() {
                val element = getNextSiblingSkippingCondition(SKIP_WHITE_SPACE_AND_COMMENT)

                println(element)
                println(element is GoMethodSpec)
                println(element?.javaClass?.name)

                return when (element) {
                    is GoPackageClause -> element.takeIf { parent is GoFile }
                    is GoTypeDeclaration -> element.innerOwner
                    is GoMethodDeclaration -> element
                    is GoMethodSpec -> element
                    is GoVarDeclaration -> element.innerOwner
                    else -> null
                }
            }
    }

}