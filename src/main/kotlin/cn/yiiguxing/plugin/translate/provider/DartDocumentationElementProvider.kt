package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.elementType
import cn.yiiguxing.plugin.translate.util.findChild
import cn.yiiguxing.plugin.translate.util.getNextSiblingSkippingCondition
import cn.yiiguxing.plugin.translate.util.getPrevSiblingSkippingCondition
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.DartTokenTypesSets
import com.jetbrains.lang.dart.psi.DartComponentName
import com.jetbrains.lang.dart.psi.DartDocComment

class DartDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val element = psiFile.findElementAt(offset) ?: return null
        val type = element.elementType
        val docElement = when {
            element is PsiComment && type == DartTokenTypesSets.SINGLE_LINE_DOC_COMMENT -> element
            element.parent is DartDocComment -> element.parent
            else -> return null
        } as PsiComment

        return docElement.takeIf { it.owner != null }
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return (documentationElement as? PsiComment)?.owner
    }

    companion object {
        private val SKIPPING_CONDITION: (PsiElement) -> Boolean = {
            it is PsiWhiteSpace || (it is PsiComment && it !is DartDocComment)
        }

        private val DART_COMPONENT_NAME_CONDITION: (PsiElement) -> Boolean = { it is DartComponentName }

        /**
         * 向上检查是否存在多行文档注释
         */
        private fun PsiComment.checkPreviousComments(): Boolean {
            return getPrevSiblingSkippingCondition(SKIPPING_CONDITION) !is DartDocComment
        }

        /**
         * 找到注释目标
         */
        private val PsiComment.owner: PsiElement?
            get() {
                // 文档注释类型中，多行注释是最高的优先级。
                // 如果当前注释不是多行注释(DartDocComment)，
                // 则向上寻找，如果上方有多行注释，则说明当前的注释是无效的。
                if (this !is DartDocComment && !checkPreviousComments()) {
                    return null
                }

                val sibling = getNextSiblingSkippingCondition(SKIPPING_CONDITION)
                    ?.takeUnless { it is DartDocComment }
                    ?: return null
                return when (sibling.elementType) {
                    DartTokenTypes.CLASS_DEFINITION,
                    DartTokenTypes.FUNCTION_DECLARATION_WITH_BODY,
                    DartTokenTypes.FUNCTION_DECLARATION_WITH_BODY_OR_NATIVE -> sibling
                    DartTokenTypes.VAR_DECLARATION_LIST -> sibling.findChild(DART_COMPONENT_NAME_CONDITION)
                    else -> null
                }
            }
    }
}