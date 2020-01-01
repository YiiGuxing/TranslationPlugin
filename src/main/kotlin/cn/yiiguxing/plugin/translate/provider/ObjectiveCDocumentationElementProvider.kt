package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.findChild
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import com.intellij.psi.*
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

class ObjectiveCDocumentationElementProvider : DocumentationElementProvider {
    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val element = psiFile.findElementAt(offset)
        return (element as? PsiComment)?.takeIf { it.owner != null }
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return (documentationElement as? PsiComment)?.owner
    }

    companion object {
        private val IS_OC_STRUCT: (PsiElement) -> Boolean = { it is OCStruct }

        private val SKIPPING_CONDITION: (PsiElement) -> Boolean = {
            (it is PsiWhiteSpace && it.text.count { char -> char == '\n' } == 1) ||
                    (it is PsiComment && it !is PsiDocCommentBase)
        }

        private val DOC_COMMENT_SKIPPING_CONDITION: (PsiElement) -> Boolean = {
            it is PsiWhiteSpace || it is PsiComment
        }

        private val PsiComment.owner: PsiElement?
            get() {
                if (this is PsiDocCommentBase) {
                    innerOwner?.let { return it }
                }

                val condition = if (this is PsiDocCommentBase) DOC_COMMENT_SKIPPING_CONDITION else SKIPPING_CONDITION
                return when (val element = getNextSiblingSkippingCondition(condition)) {
                    is OCDeclaration -> element.innerOwner
                    is OCDeclarationStatement -> element.declaration.innerOwner
                    else -> null
                }
            }

        private val PsiDocCommentBase.innerOwner: PsiElement?
            get() = owner?.let { owner -> return (owner as? OCDeclaration)?.innerOwner ?: owner }

        private val OCDeclaration.innerOwner: PsiElement?
            get() = if (type is OCStructType) {
                findChild(condition = IS_OC_STRUCT)
            } else {
                declarators.firstOrNull()
            }
    }

}