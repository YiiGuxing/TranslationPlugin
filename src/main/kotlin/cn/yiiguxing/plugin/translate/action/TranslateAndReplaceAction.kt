package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationNotifications
import cn.yiiguxing.plugin.translate.ui.SpeedSearchListPopupStep
import cn.yiiguxing.plugin.translate.ui.showListPopup
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.progress.EmptyProgressIndicatorBase
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.ui.JBColor
import com.intellij.util.concurrency.EdtScheduledExecutorService
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.text.JTextComponent

/**
 * 翻译并替换
 */
class TranslateAndReplaceAction : AutoSelectAction(true, NON_WHITESPACE_CONDITION) {

    init {
        isEnabledInModalContext = true
        templatePresentation.text = adaptedMessage("action.TranslateAndReplaceAction.text")
        templatePresentation.description = message("action.TranslateAndReplaceAction.description")
    }

    override val selectionMode: SelectionMode
        get() = Settings.autoSelectionMode

    override val AnActionEvent.editor: Editor?
        get() {
            val dataContext = dataContext
            val editor = CommonDataKeys.EDITOR.getData(dataContext)
            return if (editor != null) {
                editor
            } else {
                val data = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext)
                if (data is JTextComponent) {
                    val project = CommonDataKeys.PROJECT.getData(dataContext)
                    TextComponentEditorImpl(project, data)
                } else null
            }
        }

    override fun onUpdate(e: AnActionEvent): Boolean {
        val editor = e.editor?.takeIf { it.document.isWritable } ?: return false
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            return showReplacementActionInContextMenu(e) &&
                    selectionModel.selectedText?.any(JAVA_IDENTIFIER_PART_CONDITION) ?: false
        }
        return mayTranslateWithNoSelection(e) && showReplacementActionInContextMenu(e) && super.onUpdate(e)
    }

    override fun onActionPerformed(event: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val project = editor.project ?: CommonDataKeys.PROJECT.getData(event.dataContext)
        val document = editor.document
        val isWritable =
            project?.let { ReadonlyStatusHandler.ensureDocumentWritable(it, document) } ?: document.isWritable
        if (!isWritable) {
            return
        }
        if (!TranslateService.translator.checkConfiguration()) {
            return
        }

        val language = event.getData(LangDataKeys.LANGUAGE)
        val editorRef = WeakReference(editor)
        val text = editor.document.getText(selectionRange)
            .takeIf { it.isNotBlank() && it.any(JAVA_IDENTIFIER_PART_CONDITION) }
            ?: return
        val processedText = text.processBeforeTranslate() ?: text
        val primaryLanguage = TranslateService.translator.primaryLanguage

        val indicatorTitle = message("action.TranslateAndReplaceAction.description")
        val progressIndicator = BackgroundableProcessIndicator(project, indicatorTitle, null, "", true)
        progressIndicator.text = message("action.TranslateAndReplaceAction.task.text")
        progressIndicator.text2 = message("action.TranslateAndReplaceAction.task.text2", processedText)
        progressIndicator.addStateDelegate(ProcessIndicatorDelegate(progressIndicator))

        fun checkProcessCanceledAndEditorDisposed(): Boolean {
            if (progressIndicator.isCanceled) {
                // no need to finish the progress indicator,
                // because it's already finished in the delegate.
                return true
            }
            if (editorRef.get().let { it == null || it.isDisposed }) {
                progressIndicator.processFinish()
                return true
            }
            return false
        }

        fun translate(targetLang: Lang, reTranslate: Boolean = false) {
            if (checkProcessCanceledAndEditorDisposed()) {
                return
            }
            TranslateService.translate(processedText, Lang.AUTO, targetLang, object : TranslateListener {
                override fun onSuccess(translation: Translation) {
                    if (checkProcessCanceledAndEditorDisposed()) {
                        return
                    }
                    if (reTranslate
                        && translation.srcLang == Lang.ENGLISH
                        && primaryLanguage != Lang.ENGLISH
                        && targetLang == Lang.ENGLISH
                    ) {
                        val delay = TranslateService.translator.intervalLimit
                        if (delay <= 0) {
                            translate(primaryLanguage)
                        } else {
                            EdtScheduledExecutorService.getInstance()
                                .schedule({ translate(primaryLanguage) }, delay.toLong(), TimeUnit.MILLISECONDS)
                        }
                    } else {
                        progressIndicator.processFinish()
                        val elementsToReplace = createReplaceElements(translation, language)
                        editorRef.get()?.showResultsIfNeeds(project, selectionRange, text, elementsToReplace)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (checkProcessCanceledAndEditorDisposed()) {
                        return
                    }

                    progressIndicator.processFinish()
                    editorRef.get()?.showResultsIfNeeds(project, selectionRange, text, emptyList())
                    TranslationNotifications.showTranslationErrorNotification(
                        project, message("translate.and.replace.notification.title"), null, throwable
                    )
                }
            })
        }

        if (Settings.selectTargetLanguageBeforeReplacement) {
            editor.showTargetLanguagesPopup { translate(it) }
        } else {
            val targetLang = if (processedText.any(NON_LATIN_CONDITION)) Lang.ENGLISH else primaryLanguage
            translate(targetLang, true)
        }
    }


    private class ProcessIndicatorDelegate(
        private val progressIndicator: BackgroundableProcessIndicator,
    ) : EmptyProgressIndicatorBase(), ProgressIndicatorEx {
        override fun cancel() {
            // 在用户取消的时候使`progressIndicator`立即结束并且不再显示，否则需要等待任务结束才能跟着结束
            progressIndicator.processFinish()
        }

        override fun isCanceled(): Boolean = true
        override fun finish(task: TaskInfo) = Unit
        override fun isFinished(task: TaskInfo): Boolean = true
        override fun wasStarted(): Boolean = false
        override fun processFinish() = Unit
        override fun initStateFrom(indicator: ProgressIndicator) = Unit

        override fun addStateDelegate(delegate: ProgressIndicatorEx) {
            throw UnsupportedOperationException()
        }
    }


    private companion object {

        /** 谷歌翻译的空格符：`  -   　` */
        val SPACES = Regex("[\u00a0\u2000-\u200a\u202f\u205f\u3000]")

        val HIGHLIGHT_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFF0000, 0xFF0000)
        }

        fun String.fixWhitespace() = replace(SPACES, " ")

        fun Editor.showTargetLanguagesPopup(onChosen: (Lang) -> Unit) {
            val appStorage = AppStorage
            val languages = TranslateService.translator.supportedTargetLanguages.sortedByDescending {
                if (it == Lang.AUTO) Int.MAX_VALUE else appStorage.getLanguageScore(it)
            }
            val index = languages.indexOf(appStorage.lastReplacementTargetLanguage)

            val step = object : SpeedSearchListPopupStep<Lang>(languages, title = message("title.targetLanguage")) {
                override fun getTextFor(value: Lang): String = value.langName
                override fun onChosen(selectedValue: Lang, finalChoice: Boolean): PopupStep<*>? {
                    onChosen(selectedValue)
                    appStorage.accumulateLanguageScore(selectedValue)
                    appStorage.lastReplacementTargetLanguage = selectedValue
                    return super.onChosen(selectedValue, true)
                }
            }
            if (index >= 0) {
                step.defaultOptionIndex = index
            }

            showListPopup(step, 10)
        }

        fun Editor.canShowPopup(selectionRange: TextRange, targetText: String): Boolean {
            return selectionRange.endOffset <= document.textLength &&
                    targetText == document.getText(selectionRange) &&
                    selectionRange.containsOffset(caretModel.offset)
        }

        fun Editor.tryReplace(selectionRange: TextRange, elementsToReplace: List<String>): Boolean {
            return if (elementsToReplace.size == 1 && Settings.autoReplace) {
                replaceText(selectionRange, elementsToReplace.first())
                true
            } else false
        }

        fun Editor.checkSelection(selectionRange: TextRange): Boolean {
            val startOffset = selectionRange.startOffset
            val endOffset = selectionRange.endOffset
            with(selectionModel) {
                if (hasSelection()) {
                    if (selectionStart != startOffset || selectionEnd != endOffset) {
                        return false
                    }
                } else {
                    setSelection(startOffset, endOffset)
                }
            }

            scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            caretModel.moveToOffset(endOffset)

            return true
        }

        fun Editor.beforeShowPopup(
            selectionRange: TextRange,
            targetText: String,
            elementsToReplace: List<String>
        ): Boolean {
            if (isDisposed) {
                return false
            }
            if (!canShowPopup(selectionRange, targetText)) {
                return false
            }
            if (tryReplace(selectionRange, elementsToReplace)) {
                return false
            }
            if (!checkSelection(selectionRange)) {
                return false
            }
            return true
        }

        fun Editor.showResultsIfNeeds(
            project: Project?,
            selectionRange: TextRange,
            targetText: String,
            elements: List<String>
        ) {
            if (!beforeShowPopup(selectionRange, targetText, elements)) {
                return
            }

            if (this is TextComponentEditor) {
                showListPopup(selectionRange, elements)
            } else if (project != null) {
                showLookup(project, selectionRange, elements)
            }
        }

        fun Editor.showLookup(project: Project, selectionRange: TextRange, elementsToReplace: List<String>) {
            val markupModel = markupModel
            val lookupElements = elementsToReplace.map(LookupElementBuilder::create).toTypedArray()
            val lookup = LookupManager.getInstance(project).showLookup(this, *lookupElements) ?: return
            val highlighter = markupModel.addHighlight(selectionRange)

            lookup.addLookupListener(object : LookupListener {
                override fun itemSelected(event: LookupEvent) {
                    markupModel.removeHighlighter(highlighter)
                }

                override fun lookupCanceled(event: LookupEvent) {
                    selectionModel.removeSelection()
                    markupModel.removeHighlighter(highlighter)
                }
            })
        }

        fun Editor.replaceText(range: TextRange, text: String) {
            ApplicationManager.getApplication().invokeLater({
                WriteAction.run<Throwable> {
                    document.startGuardedBlockChecking()
                    try {
                        WriteCommandAction.runWriteCommandAction(
                            project,
                            "Translate And Replace",
                            "Translate And Replace - Replace Text",
                            { document.replaceString(range.startOffset, range.endOffset, text) }
                        )
                        selectionModel.removeSelection()
                        caretModel.moveToOffset(range.startOffset + text.length)
                        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                    } finally {
                        document.stopGuardedBlockChecking()
                    }
                }
            }, ModalityState.current())
        }

        fun MarkupModel.addHighlight(selectionRange: TextRange): RangeHighlighter {
            return addRangeHighlighter(
                selectionRange.startOffset,
                selectionRange.endOffset,
                HighlighterLayer.SELECTION - 1,
                HIGHLIGHT_ATTRIBUTES,
                HighlighterTargetArea.EXACT_RANGE
            ).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }
        }

        fun TextComponentEditor.showListPopup(selectionRange: TextRange, elements: List<String>) {
            val step = object : SpeedSearchListPopupStep<String>(elements) {
                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    replaceText(selectionRange, selectedValue)
                    return super.onChosen(selectedValue, true)
                }
            }
            showListPopup(step, 10)
        }

        fun createReplaceElements(translation: Translation, language: Language?): List<String> {
            val translationSet = translation.dictDocument?.translations?.toMutableSet() ?: mutableSetOf()
            translation.translation?.let { translationSet.add(it) }
            val items = translationSet.asSequence()
                .filter { it.isNotEmpty() }
                .map { it.fixWhitespace() }
                .toList()

            if (items.isEmpty()) {
                return emptyList()
            }

            if (translation.targetLang != Lang.ENGLISH || language == PlainTextLanguage.INSTANCE) {
                return items.filter { it.isNotBlank() }
            }

            val camel = LinkedHashSet<String>()
            val pascal = LinkedHashSet<String>()
            val original = LinkedHashSet<String>()

            val camelBuilder = StringBuilder()
            val pascalBuilder = StringBuilder()

            val lowerWithSeparatorBuilders = Settings.separators.map { it to StringBuilder() }
            val withSeparator = Settings.separators.map { it to LinkedHashSet<String>() }.toMap()

            for (item in items) {
                original.add(item)
                if (item.length > 50) {
                    continue
                }

                val words: List<String> = StringUtil.getWordsIn(item)
                if (words.isEmpty()) {
                    continue
                }

                camelBuilder.setLength(0)
                pascalBuilder.setLength(0)
                lowerWithSeparatorBuilders.forEach { it.second.setLength(0) }

                build(words, camelBuilder, pascalBuilder, lowerWithSeparatorBuilders)

                camel.add(camelBuilder.toString())
                pascal.add(pascalBuilder.toString())
                for ((separator, builder) in lowerWithSeparatorBuilders) {
                    withSeparator.getValue(separator).apply {
                        add(builder.toString())
                        add(builder.toString().uppercase())
                    }
                }
            }

            return LinkedHashSet<String>()
                .apply {
                    addAll(camel)
                    addAll(pascal)
                    for ((_, elements) in withSeparator) {
                        addAll(elements)
                    }
                    addAll(original)
                }
                .filter { it.isNotBlank() }
        }

        fun build(
            words: List<String>,
            camel: StringBuilder,
            pascal: StringBuilder,
            lowerWithSeparator: List<Pair<Char, StringBuilder>>
        ) {
            for (i in words.indices) {
                val word = if (i == 0) words[i].sanitizeJavaIdentifierStart() else words[i]
                val lowerCase = word.lowercase(Locale.getDefault())
                for ((separator, builder) in lowerWithSeparator) {
                    if (i > 0) {
                        builder.append(separator)
                    }
                    builder.append(lowerCase)
                }

                val capitalized = StringUtil.capitalizeWithJavaBeanConvention(word)
                camel.append(if (i == 0) lowerCase else capitalized)
                pascal.append(capitalized)
            }
        }

        fun String.sanitizeJavaIdentifierStart(): String {
            return if (Character.isJavaIdentifierStart(this[0])) this else "_$this"
        }
    }
}
