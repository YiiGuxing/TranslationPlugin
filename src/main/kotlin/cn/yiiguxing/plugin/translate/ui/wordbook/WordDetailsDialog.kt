package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordDetailsDialogForm
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.util.ui.JBUI
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentAdapter as EditorDocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent as EditorDocumentEvent

/**
 * Word details dialog.
 *
 * Created by Yii.Guxing on 2019/09/06.
 */
class WordDetailsDialog(
    project: Project?,
    private var word: WordBookItem,
    private val tags: Set<String>
) : WordDetailsDialogForm(project) {

    private var tagsString: String = ""

    private val tagsField: TextFieldWithAutoCompletion<String> = object : TextFieldWithAutoCompletion<String>(
        project, StringsCompletionProvider(tags, null), false, null
    ) {
        override fun createEditor(): EditorEx = super.createEditor().apply { install() }
    }

    init {
        isModal = true
        title = message("word.details.title")
        horizontalStretch = 1.33f
        verticalStretch = 1.25f
        Disposer.register(disposable, ttsButton)

        tagsPanel.add(tagsField)
        setWord(word)
        initActions()
        init()
    }

    private val isModified: Boolean
        get() = (phoneticField.text ?: "") != (word.phonetic ?: "") ||
                (explanationView.text ?: "") != (word.explanation ?: "") ||
                tagsField.text.trim(*TRIM_CHARS) != tagsString

    override fun createActions(): Array<Action> = emptyArray()

    private fun initActions() {
        registerEnterAction(tagsField)
        registerEnterAction(phoneticField)
        closeButton.addActionListener { close(OK_EXIT_CODE) }
        saveEditingButton.addActionListener { saveEditing() }
        cancelEditingButton.addActionListener {
            phoneticField.text = word.phonetic
            explanationView.text = word.explanation
            tagsField.text = tagsString
        }

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = checkModification()
        }
        phoneticField.document.addDocumentListener(listener)
        explanationView.document.addDocumentListener(listener)
    }

    private fun EditorEx.install() {
        setFontSize(JBUI.scale(14))

        var toShowHintRef = true
        document.addDocumentListener(object : EditorDocumentAdapter() {
            override fun documentChanged(e: EditorDocumentEvent?) {
                checkModification()
                toShowHintRef = false
            }
        })

        addFocusListener(object : FocusChangeListener {
            override fun focusLost(editor: Editor) {
                Application.runWriteAction {
                    document.fixSeparator()
                }
            }

            override fun focusGained(editor: Editor) {
                if (toShowHintRef && editor.document.text.isEmpty()) {
                    invokeLater {
                        HintManager.getInstance().showInformationHint(editor, message("word.details.tags.hit"))
                    }
                }
            }
        })
    }

    private fun registerEnterAction(component: JComponent) {
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val focusTarget = if (isModified) saveEditingButton else closeButton
                IdeFocusManager.findInstance().requestFocus(focusTarget, true)
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), component, disposable)
    }

    private fun checkModification() {
        isModified.let {
            saveEditingButton.isVisible = it
            cancelEditingButton.isVisible = it
        }
    }

    private fun setWord(word: WordBookItem) {
        this.word = word
        wordView.text = word.word
        languageLabel.text = word.sourceLanguage.langName
        ttsButton.dataSource { word.word to word.sourceLanguage }
        phoneticField.text = word.phonetic
        explanationLabel.text = message("word.language.explanation", word.targetLanguage.langName)
        explanationView.text = word.explanation
        explanationView.caretPosition = 0

        tagsString = word.tags.joinToString(", ")
        tagsField.text = tagsString
    }

    private fun saveEditing() {
        if (isModified) {
            val newWord = word.copy(
                phonetic = phoneticField.text,
                explanation = explanationView.text,
                tags = tagsField.text.split(REGEX_SEPARATOR).filter { it.isNotEmpty() }.toSet()
            )
            if (WordBookService.updateWord(newWord)) {
                setWord(newWord)
            }
        }
    }

    companion object {
        private val REGEX_SEPARATOR = "[\\s,]+".toRegex()

        private const val TAG_SEPARATOR = ", "

        private val TRIM_CHARS = charArrayOf(',', ' ')

        private fun Document.fixSeparator() {
            setText(text.replace(REGEX_SEPARATOR, TAG_SEPARATOR).trim(*TRIM_CHARS))
        }
    }
}