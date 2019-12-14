package cn.yiiguxing.plugin.translate.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

private val PREV_SIBLING: PsiElement.() -> PsiElement? = { prevSibling }
private val NEXT_SIBLING: PsiElement.() -> PsiElement? = { nextSibling }


/**
 * 元素类型
 */
val PsiElement.elementType: IElementType get() = node.elementType

/**
 * 从[PsiFile]中在指定[offset]处查找类型为[type]的元素并返回，如果未找到则返回`null`
 */
fun <T : PsiElement> PsiFile.findElementOfTypeAt(offset: Int, type: Class<T>): T? {
    val offsetElement = findElementAt(offset) ?: return null
    return PsiTreeUtil.getParentOfType(offsetElement, type)
}

/**
 * 查找当前元素满足指定[条件][condition]的父元素
 */
tailrec fun PsiElement.findParent(condition: (PsiElement) -> Boolean): PsiElement? {
    val parent: PsiElement = parent ?: return null
    if (condition(parent)) {
        return parent
    }
    if (parent is PsiFile) {
        return null
    }

    return parent.findParent(condition)
}

/**
 * 查找当前元素满足指定[条件][condition]的子元素
 */
fun PsiElement.findChild(condition: (PsiElement) -> Boolean): PsiElement? {
    var child: PsiElement? = firstChild
    while (child != null) {
        if (condition(child)) {
            return child
        } else {
            child.findChild(condition)?.let {
                return@findChild it
            }
        }
        child = child.nextSibling
    }

    return null
}

private tailrec fun PsiElement.getSiblingSkippingCondition(
    nextSibling: PsiElement.() -> PsiElement?,
    condition: (PsiElement) -> Boolean
): PsiElement? {
    val sibling = nextSibling() ?: return null
    return if (!condition(sibling)) sibling else sibling.getSiblingSkippingCondition(nextSibling, condition)
}

/**
 * 返回左兄弟元素并跳过满足指定[条件][condition]的元素
 */
fun PsiElement.getPrevSiblingSkippingCondition(condition: (PsiElement) -> Boolean): PsiElement? {
    return getSiblingSkippingCondition(PREV_SIBLING, condition)
}

/**
 * 返回右兄弟元素并跳过满足指定[条件][condition]的元素
 */
fun PsiElement.getNextSiblingSkippingCondition(condition: (PsiElement) -> Boolean): PsiElement? {
    return getSiblingSkippingCondition(NEXT_SIBLING, condition)
}