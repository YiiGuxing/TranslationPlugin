package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.findChild
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCDeclarationStatement
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.types.OCStructType

class ObjectiveCDocumentationElementProvider : AbstractDocumentationElementProvider() {

    override val PsiComment.isDocComment: Boolean
        get() = this@isDocComment is PsiDocCommentBase

    override val PsiComment.documentationOwner: PsiElement?
        get() = (this@documentationOwner as PsiDocCommentBase).innerOwner

    companion object {
        private val IS_OC_STRUCT: (PsiElement) -> Boolean = { it is OCStruct }

        private val SKIPPING_CONDITION: (PsiElement) -> Boolean = {
            (it is PsiWhiteSpace && it.text.count { char -> char == '\n' } <= 1) ||
                    (it is PsiComment && it !is PsiDocCommentBase)
        }

        private val DOC_COMMENT_SKIPPING_CONDITION: (PsiElement) -> Boolean = {
            it is PsiWhiteSpace || it is PsiComment
        }

        private val PsiDocCommentBase.innerOwner: PsiElement?
            get() {
                findOwner()?.let { return it }

                return when (val element = getNextSiblingSkippingCondition(DOC_COMMENT_SKIPPING_CONDITION)) {
                    is OCDeclaration -> element.innerOwner
                    is OCDeclarationStatement -> element.declaration.innerOwner
                    else -> null
                }
            }

        /*
        * 此实现包含普通注释的翻译，然，普通注释在文档中被包裹在`PRE`块内而不会被翻译，因此不用也罢。
        */
        @Suppress("unused")
        private val PsiComment.innerOwner: PsiElement?
            get() {
                if (this is PsiDocCommentBase) {
                    findOwner()?.let { return it }
                } else {
                    (parent as? OCDeclaration)?.innerOwner?.let { return it }
                }

                val condition = if (this is PsiDocCommentBase) DOC_COMMENT_SKIPPING_CONDITION else SKIPPING_CONDITION
                return when (val element = getNextSiblingSkippingCondition(condition)) {
                    is OCDeclaration -> element.innerOwner
                    is OCDeclarationStatement -> element.declaration.innerOwner
                    else -> null
                }
            }

        private fun PsiDocCommentBase.findOwner(): PsiElement? {
            return owner?.let { owner -> return (owner as? OCDeclaration)?.innerOwner ?: owner }
        }

        private val OCDeclaration.innerOwner: PsiElement?
            get() = if (type is OCStructType) {
                findChild(condition = IS_OC_STRUCT)
            } else {
                declarators.firstOrNull()
            }
    }

}