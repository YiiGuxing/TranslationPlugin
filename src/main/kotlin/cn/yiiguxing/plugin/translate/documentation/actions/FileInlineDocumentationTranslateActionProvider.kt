package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.editor
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.ITPApplicationService
import com.intellij.codeInsight.actions.ReaderModeSettings
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AnimatedIcon
import icons.TranslationIcons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private val FILE_TRANSLATION_JOB_KEY = Key.create<Job>("translation.inlineDocumentation.fileTranslationJob")

class FileInlineDocumentationTranslateActionProvider : InspectionWidgetActionProvider {
    override fun createAction(editor: Editor): AnAction? {
        val project: Project? = editor.project
        return if (project == null || project.isDefault) null
        else FileInlineDocumentationTranslateAction()
    }

    private class FileInlineDocumentationTranslateAction : AnAction(
        { message("action.FileInlineDocumentationTranslateAction.text") },
        Presentation.NULL_STRING,
        TranslationIcons.TranslationSmall
    ) {

        private var Editor.translationJob: Job?
            get() = getUserData(FILE_TRANSLATION_JOB_KEY)
            set(value) {
                putUserData(FILE_TRANSLATION_JOB_KEY, value)
                ActivityTracker.getInstance().inc()
            }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun update(e: AnActionEvent) {
            val presentation = e.presentation
            presentation.isEnabledAndVisible = false
            if (!Experiments.getInstance().isFeatureEnabled("editor.reader.mode")) {
                return
            }

            val project = e.project?.takeIf { it.isInitialized } ?: return
            val editor = e.editor ?: return
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)?.virtualFile ?: return
            if (!ReaderModeSettings.getInstance(project).enabled ||
                !ReaderModeSettings.matchMode(project, file, editor)
            ) {
                return
            }

            @Suppress("UnstableApiUsage")
            if (!DocRenderManager.isDocRenderingEnabled(editor)) {
                return
            }

            presentation.icon = when (editor.translationJob?.isCompleted) {
                false -> AnimatedIcon.Default.INSTANCE
                else -> TranslationIcons.TranslationSmall
            }
            presentation.isEnabledAndVisible = true
        }

        override fun actionPerformed(e: AnActionEvent) {
            val editor = e.editor ?: return
            val project = e.project ?: return

            editor.translationJob?.let { job ->
                if (job.isActive) {
                    job.cancel()
                }

                editor.translationJob = null
                return@actionPerformed
            }

            //TODO notify the user if there are too many comments to translate.

            editor.translationJob = ITPApplicationService.projectScope(project).launch {
                //TODO implement actual translation logic
                println("Translating...")
                delay(5000)
            }.apply {
                invokeOnCompletion { cause ->
                    println("Translation completed: cause=$cause")
                    editor.takeUnless { it.isDisposed }?.translationJob = null
                }
            }
        }
    }
}