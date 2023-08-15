package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.elementType
import cn.yiiguxing.plugin.translate.util.findParent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction

private val OWNER_CONDITION: (PsiElement) -> Boolean = { it is PyFile || it is PyClass || it is PyFunction }

class PythonDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        return psiFile.findElementAt(offset)
            ?.takeIf { it.elementType == PyTokenTypes.DOCSTRING }
    }

    override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return documentationElement.findParent(OWNER_CONDITION)
    }
}