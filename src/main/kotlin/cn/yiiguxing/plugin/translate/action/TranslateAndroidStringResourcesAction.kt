package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.ui.LanguageListModel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TranslateAndroidStringResourcesAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val file = CommonDataKeys.PSI_FILE.getData(e.dataContext)
        e.presentation.isEnabledAndVisible =
            TranslateService.translator.id == TranslationEngine.GOOGLE.id && file.isStringsFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return
        file.parent?.name
        val languages = LangDialog(project, TranslateService.translator).showAndGetLanguages() ?: return
        val progressIndicator = ProgressWindow(true, project).apply {
            title = "Translating String Resources"
            isIndeterminate = true
            start()
            text = "Translating ${file.parent!!.name}/${file.name} ..."
        }

        val now = System.currentTimeMillis()
        ReadAction.nonBlocking(Callable {
            if (file.isValid) {
                GoogleTranslator.translateDocumentation(file.text, languages.source, languages.target)
            } else null
        })
            .finishOnUiThread(ModalityState.current()) { translation ->
                translation?.translation?.takeIf { file.isValid }?.let {
                    WriteCommandAction.runWriteCommandAction(project) {
                        FileDocumentManager.getInstance().getDocument(file.virtualFile)!!.setText(it)
                        ReformatCodeProcessor(file, false).run()
                    }
                    OpenFileDescriptor(project, file.virtualFile).navigate(true)
                }
            }
            .wrapProgress(progressIndicator)
            .expireWhen { System.currentTimeMillis() - now > 5000 }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onError {
                Notifications.showErrorNotification(
                    project,
                    "String Resources Translation",
                    "String Resources Translation",
                    it.message ?: "",
                    it
                )
            }
            .onProcessed { progressIndicator.stop() }
    }

    class LangDialog(project: Project, translator: Translator) : DialogWrapper(project) {

        private var result: LanguagePair? = null

        private val fromLanguage: ComboBox<Lang> =
            ComboBox(LanguageListModel.sorted(translator.supportedSourceLanguages, Lang.AUTO)).apply {
                renderer = SimpleListCellRenderer.create("") { it.langName }
            }

        private val toLanguage: ComboBox<Lang> =
            ComboBox(LanguageListModel.sorted(translator.supportedTargetLanguages, null)).apply {
                renderer = SimpleListCellRenderer.create("") { it.langName }
            }

        init {
            title = "Translate String Resources"
            isModal = true
            setResizable(false)
            init()
        }

        override fun createCenterPanel(): JComponent = JPanel().apply {
            layout = UI.migLayout()

            add(JLabel("Translate from"))
            add(fromLanguage, UI.fillX().wrap())

            add(JLabel("Translate into"))
            add(toLanguage, UI.fillX().wrap())
        }

        override fun doValidate(): ValidationInfo {
            return ValidationInfo("Please choose a language.", toLanguage).withOKEnabled()
        }

        override fun doOKAction() {
            result = LanguagePair(fromLanguage.selected!!, toLanguage.selected!!)
            super.doOKAction()
        }

        fun showAndGetLanguages(): LanguagePair? {
            showAndGet()
            return result
        }
    }

    private companion object {
        val VALUES_DIR_REGEX = Regex("values(-\\S+)?")

        val PsiFile?.isStringsFile: Boolean
            get() {
                if (this == null) {
                    return false
                }
                if (this !is XmlFile) {
                    return false
                }
                if (name != "strings.xml") {
                    return false
                }
                if (parent?.parent?.name != "res") {
                    return false
                }
                if (parent?.name?.matches(VALUES_DIR_REGEX) != true) {
                    return false
                }
                return true
            }
    }
}