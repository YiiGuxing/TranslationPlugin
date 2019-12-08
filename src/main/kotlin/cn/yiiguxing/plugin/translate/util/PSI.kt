package cn.yiiguxing.plugin.translate.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

val PsiElement.elementType: IElementType get() = node.elementType

fun <T : PsiElement> PsiFile.findElementOfTypeAt(offset: Int, type: Class<T>): T? {
    val offsetElement = findElementAt(offset) ?: return null
    return PsiTreeUtil.getParentOfType(offsetElement, type)
}

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