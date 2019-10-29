package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.intention.CheckRegExpForm
import javax.swing.Action
import javax.swing.JComponent

/**
 * CheckRegExpDialog
 */
class CheckRegExpDialog(project: Project, regExp: String, private val ok: (String) -> Unit) : DialogWrapper(project) {

    private val regExpPsiFile: PsiFile
    private val document: Document
    private val checkRegExpForm: CheckRegExpForm

    init {
        title = "Check RegExp"
        setResizable(false)

        regExpPsiFile = PsiFileFactory.getInstance(project).createFileFromText(RegExpLanguage.INSTANCE, regExp)
        document = PsiDocumentManager.getInstance(project).getDocument(regExpPsiFile)!!

        val documentManager = PsiDocumentManager.getInstance(project)
        document.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent) {
                documentManager.commitDocument(e.document)
            }
        }, disposable)

        checkRegExpForm = CheckRegExpForm(regExpPsiFile)

        init()
    }

    override fun createCenterPanel(): JComponent = checkRegExpForm.rootPanel

    override fun createActions(): Array<Action> = arrayOf(okAction, cancelAction)

    override fun doOKAction() {
        regExpPsiFile.text?.let { ok(it) }
        super.doOKAction()
    }
}