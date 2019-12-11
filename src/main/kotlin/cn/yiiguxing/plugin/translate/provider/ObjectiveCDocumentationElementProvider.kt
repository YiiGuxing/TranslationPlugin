package cn.yiiguxing.plugin.translate.provider

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ObjectiveCDocumentationElementProvider : DocumentationElementProvider {
    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val element = psiFile.findElementAt(offset)
        // TODO Find documentation
        return null
    }
}