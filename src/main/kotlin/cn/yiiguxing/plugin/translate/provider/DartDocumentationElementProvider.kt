package cn.yiiguxing.plugin.translate.provider

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class DartDocumentationElementProvider : DocumentationElementProvider {
    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val element = psiFile.findElementAt(offset) ?: return null
        // TODO Check documentation element

        return null
    }
}