package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.elementType
import cn.yiiguxing.plugin.translate.util.findChildOfType
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import com.goide.psi.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

private val SKIP_WHITE_SPACE_AND_COMMENT: (PsiElement) -> Boolean = {
    (it is PsiWhiteSpace && it.text.count { char -> char == '\n' } <= 1) || it is PsiComment
}

class GoDocumentationElementProvider : AbstractDocumentationElementProvider() {

    override val PsiComment.isDocComment: Boolean
        get() = true

    override val PsiComment.isPickAtEdge: Boolean
        get() = elementType.toString() === "GO_LINE_COMMENT"

    override val PsiComment.documentationOwner: PsiElement?
        get() = when (val element = getNextSiblingSkippingCondition(SKIP_WHITE_SPACE_AND_COMMENT)) {
            null -> null

            is GoPackageClause -> element.takeIf { parent is GoFile }
            is GoMethodSpec, is GoFunctionOrMethodDeclaration -> element

            else -> when (element) {
                is GoTypeDeclaration -> GoTypeSpec::class.java to false
                is GoFieldDeclaration -> GoFieldDefinition::class.java to false
                is GoConstDeclaration -> GoConstDefinition::class.java to true
                is GoVarDeclaration -> GoVarDefinition::class.java to true
                else -> null
            }?.let { (type, depth) ->
                element.findChildOfType(type, depth)
            }
        }
}