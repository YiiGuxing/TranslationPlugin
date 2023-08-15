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
import com.intellij.codeInsight.lookup.*
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.ui.JBColor
import com.intellij.util.concurrency.EdtScheduledExecutorService
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.text.JTextComponent

/**
 * 翻译并替换
 */
class TranslateAndReplaceAction : AutoSelectAction(true, NON_WHITESPACE_CONDITION), PopupAction {

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
        val language = event.getData(LangDataKeys.LANGUAGE)
        val project = editor.project ?: CommonDataKeys.PROJECT.getData(event.dataContext)
        val document = editor.document
        val isWritable = project
            ?.let { ReadonlyStatusHandler.ensureDocumentWritable(it, document) }
            ?: document.isWritable
        if (!isWritable) {
            return
        }
        if (!TranslateService.translator.checkConfiguration()) {
            return
        }

        // 不要在这下面使用`event`，否则将可能会出现 `cannot share data context between Swing events` 错误。
        // 因为`TranslateService.translator.checkConfiguration()`可能会占用很长的时间并等待，
        // 之后`event`中的`DataContext`就可能变得不可使用了。详见：
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/207379815-AnActionEvent-cannot-share-data-context-between-Swing-events-

        val editorRef = WeakReference(editor)
        val text = editor.document.getText(selectionRange)
            .takeIf { it.isNotBlank() && it.any(JAVA_IDENTIFIER_PART_CONDITION) }
            ?: return
        val processedText = text.trim().let { if (!it.contains(WHITESPACE)) it.splitWords() else it }
        val primaryLanguage = TranslateService.translator.primaryLanguage

        val indicator = Indicator(project, editorRef).apply { setProgressText(processedText) }
        val modalityState = ModalityState.current()
        fun translate(targetLang: Lang, reTranslate: Boolean = false) {
            if (indicator.checkProcessCanceledAndEditorDisposed()) {
                return
            }
            TranslateService.translate(processedText, Lang.AUTO, targetLang, object : TranslateListener {
                override fun onSuccess(translation: Translation) {
                    if (indicator.checkProcessCanceledAndEditorDisposed()) {
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
                        indicator.processFinish()
                        val elementsToReplace = createReplaceElements(translation, language)
                        editorRef.get()?.showResultsIfNeeds(selectionRange, text, elementsToReplace)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (indicator.checkProcessCanceledAndEditorDisposed()) {
                        return
                    }

                    indicator.processFinish()
                    editorRef.get()?.showResultsIfNeeds(selectionRange, text, emptyList())
                    TranslationNotifications.showTranslationErrorNotification(
                        project, message("translate.and.replace.notification.title"), null, throwable
                    )
                }
            }, modalityState)
        }

        if (Settings.selectTargetLanguageBeforeReplacement) {
            editor.showTargetLanguagesPopup { translate(it) }
        } else {
            val targetLang = if (processedText.any(NON_LATIN_CONDITION)) Lang.ENGLISH else primaryLanguage
            translate(targetLang, true)
        }
    }


    private class Indicator(
        val project: Project?,
        val editorRef: WeakReference<Editor>
    ) : BackgroundableProcessIndicator(
        project,
        message("action.TranslateAndReplaceAction.description"),
        null,
        null,
        true
    ) {

        init {
            initDelegate()
            start()
            isIndeterminate = true
            text = message("action.TranslateAndReplaceAction.task.text")
        }

        private fun initDelegate() {
            addStateDelegate(object : AbstractProgressIndicatorExBase() {
                override fun cancel() {
                    // 在用户取消的时候使`progressIndicator`立即结束并且不再显示
                    this@Indicator.processFinish()
                }
            })
        }

        fun setProgressText(text: String) {
            text2 = message("action.TranslateAndReplaceAction.task.text2", text)
        }

        fun checkProcessCanceledAndEditorDisposed(): Boolean {
            if (isCanceled) {
                // no need to finish the progress indicator,
                // because it's already finished in the delegate.
                return true
            }
            if ((project != null && project.isDisposed) || editorRef.get().let { it == null || it.isDisposed }) {
                processFinish()
                return true
            }
            return false
        }
    }


    private class TranslationItemRenderer : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            presentation.itemText = element.lookupString.replace(CRLF, "↩")
        }
    }


    private companion object {

        /** 谷歌翻译的空格符：`  -   　` */
        val SPACES = Regex("[\u00a0\u2000-\u200a\u202f\u205f\u3000]")

        val CRLF = Regex("\r\n|\r|\n")

        val WHITESPACE = Regex("\\s+")

        val HIGHLIGHT_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFF0000, 0xFF0000)
        }

        fun String.fixWhitespace() = replace(SPACES, " ")

        fun Editor.showTargetLanguagesPopup(onChosen: (Lang) -> Unit) {
            val states = TranslationStates
            val languages = TranslateService.translator.supportedTargetLanguages.sortedByDescending {
                if (it == Lang.AUTO) Int.MAX_VALUE else states.getLanguageScore(it)
            }
            val index = languages.indexOf(states.lastReplacementTargetLanguage)

            val step = object : SpeedSearchListPopupStep<Lang>(languages, title = message("title.targetLanguage")) {
                override fun getTextFor(value: Lang): String = value.langName
                override fun onChosen(selectedValue: Lang, finalChoice: Boolean): PopupStep<*>? {
                    onChosen(selectedValue)
                    states.accumulateLanguageScore(selectedValue)
                    states.lastReplacementTargetLanguage = selectedValue
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
            return checkSelection(selectionRange)
        }

        fun Editor.showResultsIfNeeds(
            selectionRange: TextRange,
            targetText: String,
            elements: List<String>
        ) {
            if (!beforeShowPopup(selectionRange, targetText, elements)) {
                return
            }

            val project = project
            if (this is TextComponentEditor || project == null) {
                showListPopup(selectionRange, elements)
            } else if (!project.isDisposed) {
                showLookup(project, selectionRange, elements)
            }
        }

        fun Editor.showLookup(project: Project, selectionRange: TextRange, elementsToReplace: List<String>) {
            val markupModel = markupModel
            val renderer = TranslationItemRenderer()
            val lookupElements = elementsToReplace.map {
                LookupElementBuilder.create(it).withRenderer(renderer)
            }.toTypedArray()
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
            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteAction.run<Throwable> {
                        document.startGuardedBlockChecking()
                        try {
                            val offset = document.replaceString(range, text)
                            selectionModel.removeSelection()
                            caretModel.moveToOffset(offset)
                            scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                        } finally {
                            document.stopGuardedBlockChecking()
                        }
                    }
                },
                null,
                null
            )
        }

        fun Document.replaceString(range: TextRange, text: String): Int {
            val length = textLength
            val startOffset = minOf(range.startOffset, length)
            val endOffset = minOf(range.endOffset, length)
            replaceString(startOffset, endOffset, text)
            return startOffset + text.length
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

        fun Editor.showListPopup(selectionRange: TextRange, elements: List<String>) {
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
