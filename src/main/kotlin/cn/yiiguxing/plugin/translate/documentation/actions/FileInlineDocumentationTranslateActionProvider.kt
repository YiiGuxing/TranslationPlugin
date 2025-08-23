package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.editor
import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.utils.translateInlineDocumentation
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.ITPCoroutineService
import cn.yiiguxing.plugin.translate.ui.notification.banner.EditorBanner
import cn.yiiguxing.plugin.translate.ui.notification.banner.EditorBannerManager
import com.intellij.codeInsight.actions.ReaderModeSettings
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.documentation.render.DocRenderItemManager
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.AnimatedIcon
import icons.TranslationIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive


private const val TRANSLATION_ITEM_LIMIT = 50
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
            if (!isAvailable(project, editor)) {
                return
            }

            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)?.virtualFile ?: return
            if (!ReaderModeSettings.matchMode(project, file, editor)) {
                return
            }
            if (!isHighlightingCompleted(project, editor)) {
                return
            }

            presentation.icon = when (editor.translationJob?.isCompleted) {
                false -> AnimatedIcon.Default.INSTANCE
                else -> TranslationIcons.TranslationSmall
            }
            presentation.isEnabledAndVisible = true
        }

        private fun isAvailable(project: Project, editor: Editor): Boolean {
            @Suppress("UnstableApiUsage")
            return !project.isDisposed && !editor.isDisposed
                    && ReaderModeSettings.getInstance(project).enabled
                    && DocRenderManager.isDocRenderingEnabled(editor)
        }

        private fun isHighlightingCompleted(project: Project, editor: Editor): Boolean {
            val fileEditor = FileEditorManager.getInstance(project)
                .getAllEditors(editor.virtualFile ?: return false)
                .find { it is TextEditor && it.editor === editor } ?: return false
            return DaemonCodeAnalyzerEx.isHighlightingCompleted(fileEditor, project)
        }

        override fun actionPerformed(e: AnActionEvent) {
            val editor = e.editor ?: return
            editor.translationJob?.let { job ->
                if (job.isActive) {
                    job.cancel()
                }

                editor.translationJob = null
                return@actionPerformed
            }

            val renderItems = getRenderItems(editor)
            if (renderItems.size > TRANSLATION_ITEM_LIMIT) {
                showLimitBanner(editor)
            } else {
                translate(editor, renderItems)
            }
        }

        private data class RenderItem(val textToRender: String, val range: RangeMarker) {
            lateinit var translatedText: String
            var isTranslationError: Boolean = false
        }

        @Suppress("UnstableApiUsage")
        private fun getRenderItems(editor: Editor): List<RenderItem> {
            return DocRenderItemManager.getInstance().getItems(editor)
                ?.mapNotNull { item ->
                    val textToRender = item.textToRender
                    val foldRegion = item.foldRegion
                    if (!textToRender.isNullOrEmpty() && foldRegion != null) {
                        RenderItem(textToRender, foldRegion)
                    } else null
                }
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()
        }

        private fun translate(editor: Editor, renderItems: List<RenderItem>) {
            if (renderItems.isEmpty()) {
                return
            }

            val project = editor.project ?: return
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
            val language = psiFile.language
            editor.translationJob = ITPCoroutineService.projectScope(project)
                .launch(Dispatchers.IO) {
                    val result = ArrayList<RenderItem>(renderItems.size)
                    for (item in renderItems) {
                        if (!isActive) {
                            break
                        }
                        if (Documentations.isTranslated(item.textToRender)) {
                            continue
                        }

                        val (translatedText, hasError) = translateInlineDocumentation(item.textToRender, language)
                        item.translatedText = translatedText
                        item.isTranslationError = hasError
                        result += item
                    }

                    applyTranslationResult(project, editor, psiFile, result)
                }.apply {
                    invokeOnCompletion {
                        editor.takeUnless { it.isDisposed }?.translationJob = null
                    }
                }
        }

        private suspend fun applyTranslationResult(
            project: Project,
            editor: Editor,
            psiFile: PsiFile,
            result: List<RenderItem>
        ) {
            if (result.isEmpty()) {
                return
            }

            readAction {
                if (!isAvailable(project, editor) || !psiFile.isValid) {
                    return@readAction
                }

                // TODO: Apply translation result to render items
            }
        }

        private fun showLimitBanner(editor: Editor) {
            EditorBannerManager.setEditorBanner(editor) {
                status = EditorBanner.Status.WARNING
                message = message("documentation.banner.too.many.items.to.translate", TRANSLATION_ITEM_LIMIT)
                action(
                    text = message("documentation.banner.action.continue.translate"),
                    icon = TranslationIcons.TranslationSmall
                ) {
                    translate(editor, getRenderItems(editor))
                    EditorBannerManager.setEditorBanner(editor, null)
                }
            }
        }
    }
}