package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.getTranslatedDocumentation
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.provider.DocumentationElementProvider
import cn.yiiguxing.plugin.translate.ui.setContent
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.ui.popup.AbstractPopup
import com.intellij.util.ui.JBDimension
import java.awt.Dimension
import java.lang.ref.WeakReference

class TranslateDocumentationAction : PsiElementTranslateAction() {

    init {
        templatePresentation.text = adaptedMessage("action.TranslateDocumentationAction.text")
        templatePresentation.description = message("action.TranslateDocumentationAction.description")
    }

    override fun pickPsiElement(editor: Editor, psiFile: PsiFile, dataContext: DataContext): PsiElement? {
        return DocumentationElementProvider
            .forLanguage(psiFile.language)
            .findDocumentationElementAt(psiFile, editor.caretModel.offset)
    }

    override fun isEnabledForElement(editor: Editor, element: PsiElement, dataContext: DataContext): Boolean {
        return true
    }

    override fun doTranslate(editor: Editor, element: PsiElement, dataContext: DataContext) {
        val editorRef = WeakReference(editor)
        val project = editor.project
        val docCommentOwner = DocumentationElementProvider
            .forLanguage(element.language)
            .getDocumentationOwner(element)
            ?: return
        val provider = docCommentOwner.documentationProvider

        executeOnPooledThread {
            val documentationComponentRef = Ref<DocumentationComponent>()
            try {
                val doc = Application.runReadAction(Computable {
                    if (element.isValid && docCommentOwner.isValid) {
                        provider.generateDoc(docCommentOwner, element)
                    } else null
                }) ?: return@executeOnPooledThread

                Application.invokeAndWait {
                    val e = editorRef.get()?.takeUnless { it.isDisposed } ?: return@invokeAndWait
                    val documentationComponent = showPopup(e, docCommentOwner.title)
                    documentationComponentRef.set(documentationComponent)
                    Disposer.register(documentationComponent, { documentationComponentRef.set(null) })
                }

                if (documentationComponentRef.isNull) {
                    return@executeOnPooledThread
                }

                val translatedDocumentation =
                    TranslateService.translator.getTranslatedDocumentation(doc, element.language)
                invokeLater {
                    val documentationComponent = documentationComponentRef.get() ?: return@invokeLater
                    val e = editorRef.get()?.takeUnless { it.isDisposed }
                    if (element.isValid && e != null) {
                        documentationComponent.setContent(translatedDocumentation, element)
                        (documentationComponent.hint as? AbstractPopup)?.apply {
                            showInEditor(e)
                            updateSize(project)
                        }
                    } else {
                        documentationComponent.hint?.cancel()
                    }
                }
            } catch (e: Throwable) {
                showWarning(e, project)
                invokeLater {
                    documentationComponentRef.get()?.hint?.cancel()
                }
            }
        }
    }

    private fun showPopup(editor: Editor, title: String?): DocumentationComponent {
        val project = editor.project!!
        val component = DocumentationComponent(DocumentationManager.getInstance(project))
        val popupFactory = JBPopupFactory.getInstance()
        val hint = popupFactory
            .createComponentPopupBuilder(component, component)
            .setProject(project)
            .setTitle(title)
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setModalContext(false)
            .setDimensionServiceKey(project, DOCUMENTATION_POPUP_SIZE, false)
            .setKeyEventHandler { e ->
                if (AbstractPopup.isCloseRequest(e)) {
                    component.hint?.cancel()?.let { true } ?: false
                } else false
            }
            .setCancelCallback {
                component.hint?.size?.let { size ->
                    DimensionService.getInstance()
                        .setSize(DOCUMENTATION_POPUP_SIZE, size.takeIf { it.height > MIN_HEIGHT }, project)
                }
                true
            }
            .createPopup() as AbstractPopup

        Disposer.register(hint, component)

        component.hint = hint

        component.setContent(message("documentation.loading"))
        hint.showInEditor(editor)
        hint.updateSize(project, false)

        return component
    }


    companion object {
        private const val DOCUMENTATION_POPUP_SIZE = "documentation.popup.size"

        private const val MIN_HEIGHT = 50
        private val MAX_DEFAULT = JBDimension(500, 350)

        fun showWarning(e: Throwable, project: Project?) {
            invokeLater {
                val exceptionMessage = e.message ?: ""
                Notifications.showErrorNotification(
                    project,
                    message("translate.documentation.notification.title"),
                    message("translate.documentation.error", exceptionMessage),
                    e,
                    DisableAutoDocTranslationAction()
                )
            }
        }

        class DisableAutoDocTranslationAction : NotificationAction(message("translate.documentation.disable")) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                Settings.translateDocumentation = false
                notification.expire()
            }
        }

        private val PsiElement.documentationProvider: DocumentationProvider
            get() = DocumentationManager.getProviderFromElement(this)

        private val PsiElement.title: String?
            get() {
                if (IdeVersion.isIde2018OrNewer) return null

                val title = SymbolPresentationUtil.getSymbolPresentableText(this)
                @Suppress("InvalidBundleOrProperty")
                return CodeInsightBundle.message("javadoc.info.title", title ?: text)
            }

        private fun AbstractPopup.showInEditor(editor: Editor) {
            if (isDisposed) {
                return
            }
            if (isVisible) {
                setLocation(JBPopupFactory.getInstance().guessBestPopupLocation(editor))
            } else {
                showInBestPositionFor(editor)
            }
        }

        private fun AbstractPopup.updateSize(project: Project?, restore: Boolean = true) {
            val restoreSize = if (restore) {
                DimensionService.getInstance().getSize(DOCUMENTATION_POPUP_SIZE, project)
            } else null

            val sizeToSet = if (restoreSize == null) {
                val preferredSize = component.preferredSize
                val width = minOf(preferredSize.width, MAX_DEFAULT.width)
                val height = minOf(preferredSize.height, MAX_DEFAULT.height)
                Dimension(width, height)
            } else restoreSize

            if (restore) {
                getUserData(Dimension::class.java)?.let {
                    sizeToSet.width = maxOf(sizeToSet.width, it.width)
                    sizeToSet.height = maxOf(sizeToSet.height, it.height)
                }
            }

            size = sizeToSet
            setUserData(if (!restore) listOf(sizeToSet.clone()) else emptyList())
        }
    }
}