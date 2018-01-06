package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.OptionsConfigurable
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.HANZI_CONDITION
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.copyToClipboard
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.lookup.*
import com.intellij.notification.*
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
import javax.swing.event.HyperlinkEvent

/**
 * 翻译并替换
 */
class TranslateAndReplaceAction : AutoSelectAction(true, HANZI_CONDITION) {

    private val settings: Settings = Settings.instance

    override val selectionMode: SelectionMode
        get() = settings.autoSelectionMode

    override fun onUpdate(e: AnActionEvent, active: Boolean) {
        e.presentation.isEnabledAndVisible = e.editor
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
        val project = e.project ?: return
        e.getData(PlatformDataKeys.VIRTUAL_FILE)?.let {
            if (it.isReadOnly(project)) {
                return
            }
        }

        val editorRef = WeakReference(editor)
        editor.document.getText(selectionRange).takeIf { it.isNotBlank() }?.let { text ->
            TranslateService.INSTANCE.translate(text, Lang.AUTO, Lang.ENGLISH, object : TranslateListener {
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
                    editorRef.get()?.showErrorNotification(message, throwable)
                }
            })
        }
    }

    private companion object {

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

            HighlightManager.getInstance(project)
                    .addHighlight(this, selectionRange)
                    .let {
                        lookup.addLookupListener(object : LookupAdapter() {
                            override fun itemSelected(event: LookupEvent) {
                                it.dispose()
                            }

                            override fun lookupCanceled(event: LookupEvent) {
                                selectionModel.removeSelection()
                                it.dispose()
                            }
                        })
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

        fun List<RangeHighlighter>.dispose() {
            for (highlighter in this) {
                highlighter.dispose()
            }
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

        const val DISPLAY_ID = "TranslateAndReplaceAction"
        const val DESC_COPY_TO_CLIPBOARD = "CopyToClipboard"

        fun Editor.showErrorNotification(message: String, throwable: Throwable) {
            val thisRef = WeakReference(this)
            NotificationGroup(DISPLAY_ID, NotificationDisplayType.TOOL_WINDOW, true)
                    .createNotification(
                            "TranslateAndReplace",
                            """$message (<a href="$DESC_COPY_TO_CLIPBOARD">Copy to Clipboard</a>)""",
                            NotificationType.WARNING,
                            object : NotificationListener.Adapter() {
                                override fun hyperlinkActivated(notification: Notification, event: HyperlinkEvent) {
                                    notification.expire()
                                    when (event.description) {
                                        HTML_DESCRIPTION_SETTINGS -> thisRef.get()?.let {
                                            if (!it.isDisposed) {
                                                OptionsConfigurable.showSettingsDialog(it.project)
                                            }
                                        }
                                        DESC_COPY_TO_CLIPBOARD -> throwable.copyToClipboard()
                                    }
                                }
                            })
                    .let { Notifications.Bus.notify(it, project) }
        }
    }
}
