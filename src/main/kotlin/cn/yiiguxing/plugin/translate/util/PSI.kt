package cn.yiiguxing.plugin.translate.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

fun <T : PsiElement> PsiFile.findElementOfTypeAt(offset: Int, type: Class<T>): T? {
    val offsetElement = findElementAt(offset) ?: return null
    return PsiTreeUtil.getParentOfType(offsetElement, type)
}