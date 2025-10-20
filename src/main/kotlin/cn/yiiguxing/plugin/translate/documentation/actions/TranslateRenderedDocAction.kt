package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ToggleableTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.documentation.*
import cn.yiiguxing.plugin.translate.documentation.translateInlineDocumentation
import cn.yiiguxing.plugin.translate.service.ITPCoroutineService
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.util.findElementAroundOffset
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.ui.AnimatedIcon
import com.intellij.util.concurrency.ThreadingAssertions
import icons.TranslationIcons
import kotlinx.coroutines.Dispatchers

internal class TranslateRenderedDocAction(
    private val editor: Editor,
    private val renderedText: String
) : ToggleableTranslationAction() {

    override fun update(event: AnActionEvent, isSelected: Boolean) {
        val comment = getPsiFile(editor)?.let { getPsiDocComment(editor, it) }
        val isLoading = comment?.let {
            getPsiInlineDocumentationTranslationInfo(it)?.isLoading
        } ?: false

        with(event.presentation) {
            isEnabledAndVisible = comment != null
            icon = when {
                isSelected -> TranslationIcons.Translation
                isLoading -> AnimatedIcon.Default.INSTANCE
                else -> TranslationIcons.TranslationInactivated
            }
            text = adaptedMessage(
                if (isSelected) "action.TranslateRenderedDocAction.text.original"
                else "action.TranslateRenderedDocAction.text"
            )
        }
    }

    override fun isSelected(event: AnActionEvent): Boolean {
        return Documentations.isTranslated(renderedText)
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        ThreadingAssertions.assertEventDispatchThread()

        val psiFile = getPsiFile(editor) ?: return
        val comment = getPsiDocComment(editor, psiFile) ?: return
        val info = getPsiInlineDocumentationTranslationInfo(comment)
        val newInfo = when {
            info?.isLoading == true -> null
            info?.translatedText != null && !info.hasError -> when {
                state && info.translatorId == TranslateService.getInstance().translator.id -> info.disabled(false)
                state -> InlineDocTranslationInfo.loading()
                info.translatedText == renderedText -> info.disabled(true)
                else -> null
            }

            state -> InlineDocTranslationInfo.loading()
            else -> null
        }

        val pointer = comment.createSmartPointer()
        setPsiInlineDocumentationTranslationInfo(comment, newInfo)
        if (newInfo?.isLoading == true) {
            val language = psiFile.language
            val modalityState: ModalityState = ModalityState.current()
            ITPCoroutineService.projectScope(psiFile.project).launch(Dispatchers.IO) {
                var translatedInfo: InlineDocTranslationInfo? = translateInlineDocumentation(renderedText, language)
                translatedInfo = readAction {
                    @Suppress("UnstableApiUsage")
                    val element = pointer.dereference() ?: return@readAction null
                    val originInfo = getPsiInlineDocumentationTranslationInfo(element)
                    if (originInfo === newInfo) {
                        translatedInfo.also {
                            setPsiInlineDocumentationTranslationInfo(element, it)
                        }
                    } else null
                }

                translatedInfo?.let { rerender(psiFile, pointer, it, modalityState) }
            }
        } else {
            rerender(psiFile, pointer, newInfo)
        }
    }

    private fun rerender(
        psiFile: PsiFile,
        pointer: SmartPsiElementPointer<PsiDocCommentBase>,
        info: InlineDocTranslationInfo?,
        modalityState: ModalityState = ModalityState.current()
    ) {
        updateRendering(editor, psiFile, modalityState) {
            @Suppress("UnstableApiUsage")
            val element = pointer.dereference() ?: return@updateRendering true
            getPsiInlineDocumentationTranslationInfo(element) !== info
        }
    }

    private fun getPsiFile(editor: Editor): PsiFile? {
        val psiDocumentManager = PsiDocumentManager.getInstance(editor.project ?: return null)
        return psiDocumentManager.getPsiFile(editor.document)
    }

    private fun getPsiDocComment(editor: Editor, psiFile: PsiFile): PsiDocCommentBase? {
        val offset = editor.takeUnless { it.isDisposed }?.caretModel?.offset ?: return null
        return psiFile.findElementAroundOffset<PsiDocCommentBase>(offset)
    }
}