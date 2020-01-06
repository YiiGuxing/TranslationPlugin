package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.findElementOfTypeAt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

class KotlinDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiElement? {
        return psiFile.findElementOfTypeAt(offset, KDoc::class.java)
    }

}