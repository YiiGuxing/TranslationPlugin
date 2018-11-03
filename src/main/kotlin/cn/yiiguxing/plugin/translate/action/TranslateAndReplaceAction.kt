package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.lookup.*
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.lang.ref.WeakReference
import java.util.*

/**
 * 翻译并替换
 */
class TranslateAndReplaceAction : AutoSelectAction(true, NON_WHITESPACE_CONDITION) {

    override val selectionMode: SelectionMode
        get() = Settings.autoSelectionMode

    override fun onUpdate(e: AnActionEvent): Boolean {
        val editor = e.editor ?: return false
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            return selectionModel.selectedText?.any(JAVA_IDENTIFIER_PART_CONDITION) ?: false
        }
        return super.onUpdate(e)
    }

    override fun onActionPerformed(e: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val project = e.project ?: return
        e.getData(PlatformDataKeys.VIRTUAL_FILE)?.let {
            if (it.isReadOnly(project)) {
                return
            }
        }

        val language = e.getData(LangDataKeys.LANGUAGE)
        val editorRef = WeakReference(editor)
        editor.document.getText(selectionRange)
                .takeIf { it.isNotBlank() && it.any(JAVA_IDENTIFIER_PART_CONDITION) }
                ?.let { text ->
                    fun translate(targetLang: Lang) {
                        TranslateService.translate(text, Lang.AUTO, targetLang, object : TranslateListener {
                            override fun onSuccess(translation: Translation) {
                                val primaryLanguage = TranslateService.translator.primaryLanguage
                                if (translation.srcLang == Lang.ENGLISH
                                        && primaryLanguage != Lang.ENGLISH
                                        && targetLang == Lang.ENGLISH) {
                                    translate(primaryLanguage)
                                } else {
                                    val items = translation
                                            .run {
                                                dictionaries
                                                        .map { it.terms }
                                                        .flatten()
                                                        .toMutableSet()
                                                        .apply { trans?.let { add(it) } }
                                            }
                                            .asSequence()
                                            .filter { it.isNotEmpty() }
                                            .map { it.fixWhitespace() }
                                            .toList()


                                    val lookupItems = createReplaceLookupElements(language, items,
                                            translation.targetLang)
                                    invokeLater {
                                        editorRef.get()?.doReplace(selectionRange, text, lookupItems)
                                    }
                                }
                            }

                            override fun onError(message: String, throwable: Throwable) {
                                editorRef.get()?.let { editor ->
                                    invokeLater {
                                        editor.doReplace(selectionRange, text, emptyList())
                                    }
                                    Notifications.showErrorNotification(editor.project, NOTIFICATION_DISPLAY_ID,
                                            "Translate and Replace", message, throwable)
                                }
                            }
                        })
                    }

                    val targetLang = Lang.AUTO
                            .takeIf { TranslateService.translator.supportedTargetLanguages.contains(it) }
                            ?: Lang.ENGLISH
                    translate(targetLang)
                }
    }

    private companion object {

        const val NOTIFICATION_DISPLAY_ID = "Translate and Replace Error"

        /** 谷歌翻译的空格符：`0xA0` */
        const val GT_WHITESPACE_CHARACTER = ' ' // 0xA0
        /** 空格符：`0x20` */
        const val WHITESPACE_CHARACTER = ' ' // 0x20

        val HIGHLIGHT_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFF0000, 0xFF0000)
        }

        fun String.fixWhitespace() = replace(GT_WHITESPACE_CHARACTER, WHITESPACE_CHARACTER)

        fun VirtualFile.isReadOnly(project: Project): Boolean {
            return ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(this).hasReadonlyFiles()
        }

        fun Editor.doReplace(selectionRange: TextRange, targetText: String, replaceLookup: List<LookupElement>) {
            val project = project ?: return
            if (isDisposed || targetText != document.getText(selectionRange)
                    || !selectionRange.containsOffset(caretModel.offset)) {
                return
            }
            if (replaceLookup.size == 1 && Settings.autoReplace) {
                replaceText(selectionRange, replaceLookup.first().lookupString)
                return
            }

            val startOffset = selectionRange.startOffset
            val endOffset = selectionRange.endOffset
            with(selectionModel) {
                if (hasSelection()) {
                    if (selectionStart != startOffset || selectionEnd != endOffset) {
                        return
                    }
                } else {
                    setSelection(startOffset, endOffset)
                }
            }

            scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            caretModel.moveToOffset(endOffset)

            val items = replaceLookup.toTypedArray()
            val lookup = LookupManager.getInstance(project).showLookup(this, *items) ?: return
            val highlightManager = HighlightManager.getInstance(project)
            val highlighters = highlightManager.addHighlight(this, selectionRange)

            lookup.addLookupListener(object : LookupAdapter() {
                override fun itemSelected(event: LookupEvent) {
                    highlightManager.removeSegmentHighlighters(this@doReplace, highlighters)
                }

                override fun lookupCanceled(event: LookupEvent) {
                    selectionModel.removeSelection()
                    highlightManager.removeSegmentHighlighters(this@doReplace, highlighters)
                }
            })
        }

        fun Editor.replaceText(range: TextRange, text: String) {
            ApplicationManager.getApplication().runWriteAction {
                document.startGuardedBlockChecking()
                try {
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.replaceString(range.startOffset, range.endOffset, text)
                    }
                    selectionModel.removeSelection()
                    caretModel.moveToOffset(range.startOffset + text.length)
                    scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                } finally {
                    document.stopGuardedBlockChecking()
                }
            }
        }

        fun HighlightManager.addHighlight(editor: Editor, selectionRange: TextRange)
                : List<RangeHighlighter> = ArrayList<RangeHighlighter>().apply {
            addOccurrenceHighlight(
                    editor,
                    selectionRange.startOffset,
                    selectionRange.endOffset,
                    HIGHLIGHT_ATTRIBUTES,
                    0,
                    this,
                    null
            )

            for (highlighter in this) {
                highlighter.isGreedyToLeft = true
                highlighter.isGreedyToRight = true
            }
        }

        fun HighlightManager.removeSegmentHighlighters(editor: Editor, highlighters: List<RangeHighlighter>) {
            highlighters.forEach { removeSegmentHighlighter(editor, it) }
        }

        fun createReplaceLookupElements(language: Language?,
                                        items: Collection<String>,
                                        targetLang: Lang): List<LookupElement> {
            if (items.isEmpty()) {
                return emptyList()
            }

            if (targetLang != Lang.ENGLISH || language == PlainTextLanguage.INSTANCE) {
                return items.map { LookupElementBuilder.create(it) }
            }

            val camel = LinkedHashSet<LookupElement>()
            val pascal = LinkedHashSet<LookupElement>()
            val lowerWithUnder = LinkedHashSet<LookupElement>()
            val withSpace = LinkedHashSet<LookupElement>()
            val original = LinkedHashSet<LookupElement>()

            val camelBuilder = StringBuilder()
            val pascalBuilder = StringBuilder()
            val lowerWithUnderBuilder = StringBuilder()
            val withSpaceBuilder = StringBuilder()

            for (item in items) {
                original.add(LookupElementBuilder.create(item))
                if (item.length > 50) {
                    continue
                }

                val words: List<String> = StringUtil.getWordsIn(item)
                if (words.isEmpty()) {
                    continue
                }

                camelBuilder.setLength(0)
                pascalBuilder.setLength(0)
                lowerWithUnderBuilder.setLength(0)
                withSpaceBuilder.setLength(0)

                build(words, camelBuilder, pascalBuilder, lowerWithUnderBuilder, withSpaceBuilder)

                camel.add(LookupElementBuilder.create(camelBuilder.toString()))
                pascal.add(LookupElementBuilder.create(pascalBuilder.toString()))
                lowerWithUnder.add(LookupElementBuilder.create(lowerWithUnderBuilder.toString()))
                withSpace.add(LookupElementBuilder.create(withSpaceBuilder.toString()))
            }

            return LinkedHashSet<LookupElement>().apply {
                addAll(camel)
                addAll(pascal)
                addAll(lowerWithUnder)
                addAll(withSpace)
                addAll(original)
            }.toList()
        }

        fun build(words: List<String>,
                  camel: StringBuilder,
                  pascal: StringBuilder,
                  lowerWithUnder: StringBuilder,
                  withSpace: StringBuilder) {
            for (i in words.indices) {
                var word = words[i]

                if (i > 0) {
                    lowerWithUnder.append('_')
                    withSpace.append(' ')
                }

                withSpace.append(word)

                if (i == 0) {
                    word = word.sanitizeJavaIdentifierStart()
                }

                val capitalized = StringUtil.capitalizeWithJavaBeanConvention(word)
                val lowerCase = word.toLowerCase()

                camel.append(if (i == 0) lowerCase else capitalized)
                pascal.append(capitalized)
                lowerWithUnder.append(lowerCase)
            }
        }

        fun String.sanitizeJavaIdentifierStart(): String = if (Character.isJavaIdentifierStart(this[0])) {
            this
        } else {
            "_$this"
        }
    }
}
