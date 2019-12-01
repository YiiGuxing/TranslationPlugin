package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.DocumentationElementProvider
import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.ui.setContent
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.ui.popup.AbstractPopup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
        val project = editor.project
        val docCommentOwner = (if (element is PsiDocCommentBase) element.owner else element.parent) ?: return
        val provider = docCommentOwner.documentationProvider ?: return

        executeOnPooledThread {
            val documentationComponentRef = Ref<DocumentationComponent>()
            try {
                val doc = Application.runReadAction(Computable {
                    if (element.isValid && docCommentOwner.isValid) {
                        provider.generateDoc(docCommentOwner, element)
                    } else null
                }) ?: return@executeOnPooledThread

                Application.invokeAndWait {
                    val documentationComponent = showPopup(project, docCommentOwner.title)
                    documentationComponentRef.set(documentationComponent)
                    Disposer.register(documentationComponent, Disposable { documentationComponentRef.set(null) })
                }

                val translatedDocumentation = getTranslatedDocumentation(doc).fixHtml()
                invokeLater {
                    if (element.isValid) {
                        documentationComponentRef.get()?.setContent(translatedDocumentation, element)
                    }
                }
            } catch (e: Throwable) {
                invokeLater {
                    documentationComponentRef.get()?.hint?.cancel()
                    Notifications.showErrorNotification(
                        project,
                        NOTIFICATION_DISPLAY_ID,
                        "Documentation",
                        "Failed to translate documentation: ${e.message}",
                        e
                    )
                }
            }
        }
    }

    private fun getTranslatedDocumentation(documentation: String): String {
        val document = Jsoup.parse(documentation)
        val translator = TranslateService.translator
        return if (translator is GoogleTranslator) {
            translator.getTranslatedDocumentation(document)
        } else {
            translator.getTranslatedDocumentation(document)
        }
    }

    private fun Translator.getTranslatedDocumentation(document: Document): String {
        val body = document.body()
        val definition = body.selectFirst(CSS_QUERY_DEFINITION)?.apply { remove() }

        val htmlDocument = HTMLDocument().also { HTML_KIT.read(StringReader(body.html()), it, 0) }
        val formatted = htmlDocument.getText(0, htmlDocument.length).trim()
        val translation = translateDocumentation(formatted, Lang.AUTO, primaryLanguage).translation ?: ""

        val newBody = Element("body")
        definition?.let { newBody.appendChild(it) }
        Element("div")
            .addClass("content")
            .append(translation.replace("\n", "<br/>"))
            .let { newBody.appendChild(it) }

        body.replaceWith(newBody)

        return document.outerHtml()
    }

    private fun showPopup(project: Project?, title: String?): DocumentationComponent {
        val component = DocumentationComponent(DocumentationManager.getInstance(project))
        val hint = JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(component, component)
            .setProject(project)
            .setTitle(title)
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

        component.setContent(message("documentation.loading"))
        return component
    }


    companion object {
        private const val NOTIFICATION_DISPLAY_ID = "Document Translation"

        private const val NEW_JAVADOC_LOCATION_AND_SIZE = "javadoc.popup.new"

        private const val CSS_QUERY_DEFINITION = ".definition"
        private const val CSS_QUERY_PRE = "pre"

        private val HTML_HEAD_REGEX = Regex("""<(?<tag>.+?) class="(?<class>.+?)">""")
        private const val HTML_HEAD_REPLACEMENT = "<${'$'}{tag} class='${'$'}{class}'>"

        private val HTML_KIT = HTMLEditorKit()

        private val PsiElement.documentationProvider: DocumentationProvider?
            get() = DocumentationManager.getProviderFromElement(this)

        /**
         * 修复HTML格式。[DocumentationComponent]识别不了`attr="val"`的属性表达形式，只识别`attr='val'`的表达形式，导致样式显示异常。
         */
        private fun String.fixHtml(): String = replace(HTML_HEAD_REGEX, HTML_HEAD_REPLACEMENT)

        private val PsiElement.title: String?
            get() {
                if (IdeVersion.isIde2018OrNewer) return null

                val title = SymbolPresentationUtil.getSymbolPresentableText(this)
                return CodeInsightBundle.message("javadoc.info.title", title ?: text)
            }
    }
}