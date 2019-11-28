package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.provider.DocumentationElementProvider
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.io.StringReader
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

class TranslateDocumentationAction : PsiElementTranslateAction() {

    override fun pickPsiElement(editor: Editor, psiFile: PsiFile, dataContext: DataContext): PsiElement? {
        return DocumentationElementProvider
            .forLanguage(psiFile.language)
            .findDocumentationElementAt(psiFile, editor.caretModel.offset)
    }

    override fun isEnabledForElement(editor: Editor, element: PsiElement, dataContext: DataContext): Boolean {
        return true
    }

    override fun doTranslate(editor: Editor, element: PsiElement, dataContext: DataContext) {
        val docCommentOwner = (if (element is PsiDocCommentBase) element.owner else element.parent) ?: return
        val provider = docCommentOwner.documentationProvider ?: return
        val doc = provider.generateDoc(docCommentOwner, element) ?: return

        val htmlDocument = HTMLDocument().also { HTML_KIT.read(StringReader(doc), it, 0) }
        htmlDocument.getText(0, htmlDocument.length).trim()
    }


    companion object {
        private val HTML_KIT = HTMLEditorKit()

        private val PsiElement.documentationProvider: DocumentationProvider?
            get() = DocumentationManager.getProviderFromElement(this)
    }
}