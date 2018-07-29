package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
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
class TranslateAndReplaceAction : AutoSelectAction(true, NON_LATIN_CONDITION) {

    override val selectionMode: SelectionMode
        get() = Settings.autoSelectionMode

    override fun onUpdate(e: AnActionEvent, active: Boolean) {
        e.presentation.isEnabledAndVisible = active
                && e.editor
                ?.selectionModel
                ?.takeIf { it.hasSelection() }
                ?.selectedText
                ?.any(NON_LATIN_CONDITION)
                ?: active
    }

    override fun onActionPerformed(e: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val project = e.project ?: return
        e.getData(PlatformDataKeys.VIRTUAL_FILE)?.let {
            if (it.isReadOnly(project)) {
                return
            }
        }

        val editorRef = WeakReference(editor)
        editor.document.getText(selectionRange).takeIf { it.isNotBlank() }?.let { text ->
            TranslateService.translate(text, Lang.AUTO, Lang.ENGLISH, object : TranslateListener {
                override fun onSuccess(translation: Translation) {
                    val items = with(translation) {
                        dictionaries.map { it.terms }.flatten().toMutableSet().apply {
                            addAll(basicExplains)
                            trans?.let { add(it) }
                        }
                    }.filter { it.isNotEmpty() && it.matches(ITEM_FILTER_REGEX) }

                    editorRef.get()?.doReplace(selectionRange, text, items.toReplaceLookupElements())
                }

                override fun onError(message: String, throwable: Throwable) {
                    editorRef.get()?.let {
                        Notifications.showErrorNotification(it.project, NOTIFICATION_DISPLAY_ID, message, throwable)
                    }
                }
            })
        }
    }

    private companion object {

        const val NOTIFICATION_DISPLAY_ID = "TranslateAndReplaceAction"
        val ITEM_FILTER_REGEX = "[ _a-zA-Z0-9]+".toRegex()

        val HIGHLIGHT_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFF0000, 0xFF0000)
        }

        fun VirtualFile.isReadOnly(project: Project): Boolean {
            return ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(this).hasReadonlyFiles()
        }

        fun Editor.doReplace(selectionRange: TextRange, targetText: String, replaceLookup: List<LookupElement>) {
            val project = project ?: return
            if (isDisposed || targetText != document.getText(selectionRange)
                    || !selectionRange.containsOffset(caretModel.offset)) {
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

        fun List<String>.toReplaceLookupElements(): List<LookupElement> {
            if (this.isEmpty()) {
                return emptyList()
            }

            val camel = LinkedHashSet<LookupElement>()
            val pascal = LinkedHashSet<LookupElement>()
            val lowerWithUnder = LinkedHashSet<LookupElement>()
            val withSpace = LinkedHashSet<LookupElement>()

            val camelBuilder = StringBuilder()
            val pascalBuilder = StringBuilder()
            val lowerWithUnderBuilder = StringBuilder()
            val withSpaceBuilder = StringBuilder()

            for (item in this) {
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

            return with(LinkedHashSet<LookupElement>()) {
                addAll(camel)
                addAll(pascal)
                addAll(lowerWithUnder)
                addAll(withSpace)
                toList()
            }
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
