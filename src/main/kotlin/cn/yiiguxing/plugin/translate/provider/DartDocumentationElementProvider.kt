package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.elementType
import cn.yiiguxing.plugin.translate.util.findChildOfType
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import cn.yiiguxing.plugin.translate.util.getPrevSiblingSkippingCondition
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.lang.dart.DartTokenTypesSets
import com.jetbrains.lang.dart.psi.DartClassMembers
import com.jetbrains.lang.dart.psi.DartComponent
import com.jetbrains.lang.dart.psi.DartDocComment
import com.jetbrains.lang.dart.psi.DartVarDeclarationList

private val SKIPPING_CONDITION: (PsiElement) -> Boolean = {
    it is PsiWhiteSpace || (it is PsiComment && it !is DartDocComment)
}

/**
 * 向上检查是否存在多行文档注释
 */
private fun PsiComment.checkPreviousComments(): Boolean {
    return getPrevSiblingSkippingCondition(SKIPPING_CONDITION) !is DartDocComment
}

class DartDocumentationElementProvider : AbstractDocumentationElementProvider() {

    override val PsiComment.isDocComment: Boolean
        get() = this@isDocComment is DartDocComment || elementType == DartTokenTypesSets.SINGLE_LINE_DOC_COMMENT

    override val PsiComment.isPickAtEdge: Boolean
        get() = elementType == DartTokenTypesSets.SINGLE_LINE_DOC_COMMENT

    override val PsiComment.documentationOwner: PsiElement?
        get() {
            // 文档注释类型中，多行注释有最高的优先级。
            // 如果当前注释不是多行注释(DartDocComment)，则向上寻找，如果上方有多行注释，则说明当前的注释是无效的。
            // 且，最下方的多行文档注释有最高的优先级，向下寻找时如遇文档注释，则当前的注释也是无效的。

            if (this !is DartDocComment && !checkPreviousComments()) {
                return null
            }

            return when (val sibling = getNextSiblingSkippingCondition(SKIPPING_CONDITION)) {
                is DartComponent -> sibling.componentName
                is DartClassMembers,
                is DartVarDeclarationList -> sibling.findChildOfType(DartComponent::class.java)?.componentName

                else -> null
            }
        }
}