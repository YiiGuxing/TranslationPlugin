package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.model.QueryResult
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.util.HANZI_CONDITION
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.TranslationResultUtils
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.ui.JBColor
import java.util.*

/**
 * 翻译并替换
 */
class TranslateAndReplaceAction : AutoSelectAction(true, HANZI_CONDITION) {

    private val settings: Settings = Settings.instance

    override val selectionMode: SelectionMode
        get() = settings.autoSelectionMode

    override fun onUpdate(e: AnActionEvent, active: Boolean) {
        e.presentation.isEnabledAndVisible = getEditor(e)
                ?.takeIf { active }
                ?.selectionModel
                ?.takeIf { it.hasSelection() }
                ?.selectedText
                ?.let {
                    for (c in it) {
                        if (HANZI_CONDITION.value(c)) {
                            return@let true
                        }
                    }

                    false
                } ?: active
    }

    override fun onActionPerformed(e: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        val project = e.project
        if (project == null || virtualFile != null
                && ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile).hasReadonlyFiles()) {
            return
        }

        editor.document.getText(selectionRange).takeIf { it.isNotBlank() }?.let { queryText ->
            Translator.instance.translate(queryText) { _, result ->
                result?.takeIf { it.isSuccessful }
                        ?.let { getReplaceLookupElements(it) }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            ApplicationManager.getApplication().invokeLater {
                                doReplace(editor, selectionRange, queryText, it)
                            }
                        }
            }
        }
    }

    private companion object {

        const val PATTERN_FIX = "^(\\[[\\u4E00-\\u9FBF]+])+ "

        val HIGHLIGHT_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.BOXED
            effectColor = JBColor(0xFFFF0000.toInt(), 0xFFFF0000.toInt())
        }

        fun doReplace(editor: Editor,
                      selectionRange: TextRange,
                      targetText: String,
                      replaceLookup: List<LookupElement>) {
            val project = editor.project
            if (editor.isDisposed || project == null ||
                    targetText != editor.document.getText(selectionRange) ||
                    !selectionRange.containsOffset(editor.caretModel.offset)) {
                return
            }

            val selectionModel = editor.selectionModel
            val startOffset = selectionRange.startOffset
            val endOffset = selectionRange.endOffset
            if (selectionModel.hasSelection()) {
                if (selectionModel.selectionStart != startOffset || selectionModel.selectionEnd != endOffset) {
                    return
                }
            } else {
                selectionModel.setSelection(startOffset, endOffset)
            }

            editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            editor.caretModel.moveToOffset(endOffset)

            val items = replaceLookup.toTypedArray()
            val lookup = LookupManager.getInstance(project).showLookup(editor, *items) ?: return

            val highlightManager = HighlightManager.getInstance(project)
            val highlighters = addHighlight(highlightManager, editor, selectionRange)

            lookup.addLookupListener(object : LookupAdapter() {
                override fun itemSelected(event: LookupEvent?) {
                    disposeHighlight(highlighters)
                }

                override fun lookupCanceled(event: LookupEvent?) {
                    selectionModel.removeSelection()
                    disposeHighlight(highlighters)
                }
            })
        }

        fun addHighlight(highlightManager: HighlightManager,
                         editor: Editor,
                         selectionRange: TextRange): List<RangeHighlighter> = ArrayList<RangeHighlighter>().apply {
            highlightManager.addOccurrenceHighlight(
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

        fun disposeHighlight(highlighters: List<RangeHighlighter>) {
            for (highlighter in highlighters) {
                highlighter.dispose()
            }
        }

        fun getReplaceLookupElements(result: QueryResult): List<LookupElement> =
                if (result.basicExplain != null) {
                    getReplaceLookupElements(TranslationResultUtils.expandExplain(result.basicExplain?.explains))
                } else {
                    getReplaceLookupElements(result.translation)
                }

        fun getReplaceLookupElements(explains: Array<String>?): List<LookupElement> {
            if (explains == null || explains.isEmpty())
                return emptyList()

            val camel = LinkedHashSet<LookupElement>()
            val pascal = LinkedHashSet<LookupElement>()
            val lowerWithUnder = LinkedHashSet<LookupElement>()
            val capsWithUnder = LinkedHashSet<LookupElement>()
            val withSpace = LinkedHashSet<LookupElement>()

            val camelBuilder = StringBuilder()
            val pascalBuilder = StringBuilder()
            val lowerWithUnderBuilder = StringBuilder()
            val capsWithUnderBuilder = StringBuilder()
            val withSpaceBuilder = StringBuilder()

            for (explain in explains) {
                val words = fixAndSplitForVariable(explain)
                if (words == null || words.isEmpty()) {
                    continue
                }

                camelBuilder.setLength(0)
                pascalBuilder.setLength(0)
                lowerWithUnderBuilder.setLength(0)
                capsWithUnderBuilder.setLength(0)
                withSpaceBuilder.setLength(0)

                build(words, camelBuilder, pascalBuilder, lowerWithUnderBuilder, capsWithUnderBuilder, withSpaceBuilder)

                camel.add(LookupElementBuilder.create(camelBuilder.toString()))
                pascal.add(LookupElementBuilder.create(pascalBuilder.toString()))
                lowerWithUnder.add(LookupElementBuilder.create(lowerWithUnderBuilder.toString()))
                capsWithUnder.add(LookupElementBuilder.create(capsWithUnderBuilder.toString()))
                withSpace.add(LookupElementBuilder.create(withSpaceBuilder.toString()))
            }

            val result = LinkedHashSet<LookupElement>()
            result.addAll(camel)
            result.addAll(pascal)
            result.addAll(lowerWithUnder)
            result.addAll(capsWithUnder)
            result.addAll(withSpace)

            return Collections.unmodifiableList(ArrayList(result))
        }

        fun build(words: List<String>,
                  camel: StringBuilder,
                  pascal: StringBuilder,
                  lowerWithUnder: StringBuilder,
                  capsWithUnder: StringBuilder,
                  withSpace: StringBuilder) {
            for (i in words.indices) {
                var word = words[i]

                if (i > 0) {
                    lowerWithUnder.append('_')
                    capsWithUnder.append('_')
                    withSpace.append(' ')
                }

                withSpace.append(word)

                if (i == 0) {
                    word = sanitizeJavaIdentifierStart(word)
                }

                val capitalized = StringUtil.capitalizeWithJavaBeanConvention(word)
                val lowerCase = word.toLowerCase()

                camel.append(if (i == 0) lowerCase else capitalized)
                pascal.append(capitalized)
                lowerWithUnder.append(lowerCase)
                capsWithUnder.append(word.toUpperCase())
            }
        }

        fun fixAndSplitForVariable(explains: String): List<String>? {
            val (_, explain) = TranslationResultUtils.splitExplain(explains)
            if (explain.isBlank()) {
                return null
            }

            val fixed = explain.replaceFirst(PATTERN_FIX.toRegex(), "")
            return StringUtil.getWordsIn(fixed)
        }

        fun sanitizeJavaIdentifierStart(name: String): String {
            return if (Character.isJavaIdentifierStart(name[0])) name else "_" + name
        }
    }

}
