@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

private val PREV_SIBLING: PsiElement.() -> PsiElement? = { prevSibling }
private val NEXT_SIBLING: PsiElement.() -> PsiElement? = { nextSibling }

val SKIP_WHITE_SPACE: (PsiElement) -> Boolean = { it is PsiWhiteSpace }


/**
 * 元素类型
 */
val PsiElement.elementType: IElementType get() = node.elementType

/**
 * 开始偏移
 */
val PsiElement.startOffset: Int get() = textRange.startOffset

/**
 * 结束偏移
 */
val PsiElement.endOffset: Int get() = textRange.endOffset

/**
 * 从[PsiFile]中在指定[offset]处查找类型为[type]的元素并返回，如果未找到则返回`null`
 */
fun <T : PsiElement> PsiFile.findElementOfTypeAt(offset: Int, type: Class<T>): T? {
    val offsetElement = findElementAt(offset) ?: return null
    return PsiTreeUtil.getParentOfType(offsetElement, type, false)
}

/**
 * 从当前元素的子元素中查找类型为[type]的元素并返回。如果指定[depth]为`true`（默认为`false`），
 * 则从所有的子孙元素树中查找，否则仅从第一代子元素中查找。
 */
@Suppress("UNCHECKED_CAST")
fun <T : PsiElement> PsiElement.findChildOfType(type: Class<T>, depth: Boolean = false): T? {
    var child: PsiElement? = firstChild
    while (child != null) {
        if (type.isInstance(child)) {
            return child as? T
        } else if (depth) {
            child.findChildOfType(type, true)?.let {
                return@findChildOfType it
            }
        }
        child = child.nextSibling
    }

    return null
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
fun PsiElement.findChild(depth: Boolean = true, condition: (PsiElement) -> Boolean): PsiElement? {
    var child: PsiElement? = firstChild
    while (child != null) {
        if (condition(child)) {
            return child
        } else if (depth) {
            child.findChild(true, condition)?.let {
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