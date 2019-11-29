package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.provider.DocumentationElementProvider
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.AbstractPopup
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

        showInPopup(editor, element, doc)
    }

    private fun showInPopup(editor: Editor, element: PsiElement, text: String) {
        val project = editor.project
        val component = DocumentationComponent(DocumentationManager.getInstance(project))
        val hint = JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(component, component)
            .setProject(project)
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setModalContext(false)
            .setKeyEventHandler { e ->
                if (AbstractPopup.isCloseRequest(e)) {
                    component.hint?.cancel()?.let { true } ?: false
                } else false
            }
            .createPopup() as AbstractPopup

        Disposer.register(hint, component)

        component.hint = hint
        hint.dimensionServiceKey =
            if (DimensionService.getInstance().getSize(NEW_JAVADOC_LOCATION_AND_SIZE, project) != null) {
                NEW_JAVADOC_LOCATION_AND_SIZE
            } else {
                DocumentationManager.JAVADOC_LOCATION_AND_SIZE
            }

        component.setText(text, element, true)

        hint.showInBestPositionFor(editor)
    }


    companion object {
        private const val NEW_JAVADOC_LOCATION_AND_SIZE = "javadoc.popup.new"

        private val HTML_KIT = HTMLEditorKit()

        private val PsiElement.documentationProvider: DocumentationProvider?
            get() = DocumentationManager.getProviderFromElement(this)
    }
}