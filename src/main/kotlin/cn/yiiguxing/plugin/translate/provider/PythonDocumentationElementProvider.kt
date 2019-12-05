package cn.yiiguxing.plugin.translate.provider

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class PythonDocumentationElementProvider : DocumentationElementProvider {
    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        val offsetElement = psiFile.findElementAt(offset)
        // TODO check DocStringElementType

        return null
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        // TODO get owner: Function, Class or File.
        return super.getDocumentationOwner(documentationElement)
    }

}