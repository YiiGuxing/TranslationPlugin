package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.Popups
import cn.yiiguxing.plugin.translate.ui.form.WordDetailsDialogForm
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.*
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.TextFieldWithAutoCompletion
import org.jetbrains.concurrency.runAsync
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.lang.ref.WeakReference
import javax.swing.Action
import javax.swing.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentEvent as EditorDocumentEvent

/**
 * Word details dialog.
 */
class WordDetailsDialog(
    private val project: Project?,
    private var word: WordBookItem
) : WordDetailsDialogForm(project) {

    private var tagsString: String = ""

    private val tagsField: TextFieldWithAutoCompletion<String> = object : TextFieldWithAutoCompletion<String>(
        project, StringsCompletionProvider(WordBookView.instance.wordTags, null), false, null
    ) {
        init {
            font = phoneticField.font
        }

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
        get() = phoneticField.text != (word.phonetic ?: "") ||
                (explanationView.text ?: "") != (word.explanation ?: "") ||
                tagsField.text.replace(REGEX_WHITESPACE, " ").trim(*TRIM_CHARS) != tagsString

    override fun createActions(): Array<Action> = emptyArray()

    private fun initActions() {
        rootPane.defaultButton.addActionListener {
            if (closeButton.isFocusOwner) {
                close(OK_EXIT_CODE)
            } else {
                val focusTarget = if (isModified) saveEditingButton else closeButton
                IdeFocusManager.findInstance().requestFocus(focusTarget, true)
            }
        }
        saveEditingButton.addActionListener { saveEditing() }
        cancelEditingButton.addActionListener {
            phoneticField.text = word.phonetic ?: ""
            explanationView.text = word.explanation
            tagsField.text = tagsString
            IdeFocusManager.findInstance().requestFocus(closeButton, true)
        }

        phoneticField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: EditorDocumentEvent) = checkModification()
        })
        phoneticField.addFocusListener(object : FocusAdapter() {
            private var toShowHint = true

            override fun focusGained(e: FocusEvent?) {
                if (toShowHint && phoneticField.text.isEmpty()) {
                    invokeLater(expired = { phoneticField.editor.let { it == null || it.isDisposed } }) {
                        phoneticField.editor?.let {
                            HintManager.getInstance().showInformationHint(it, message("word.details.tip.phonetic"))
                        }
                    }
                    toShowHint = false
                }
            }
        })
        explanationView.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = checkModification()
        })
    }

    private fun EditorEx.install() {
        var toShowHint = true
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: EditorDocumentEvent) {
                if (e.newFragment.toString() == " ") {
                    AutoPopupController
                        .getInstance(project!!)
                        .autoPopupMemberLookup(this@install, CompletionType.SMART, null)
                }
                checkModification()
                toShowHint = false
            }
        })

        addFocusListener(object : FocusChangeListener {
            override fun focusLost(editor: Editor) {
                Application.runWriteAction {
                    document.fixSeparator()
                }
            }

            override fun focusGained(editor: Editor) {
                if (toShowHint && editor.document.text.isEmpty()) {
                    invokeLater(expired = { editor.isDisposed }) {
                        HintManager.getInstance().showInformationHint(editor, message("word.details.tags.hit"))
                    }
                }
            }
        })
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
        phoneticField.text = word.phonetic ?: ""
        explanationLabel.text = message("word.language.explanation", word.targetLanguage.langName)
        explanationView.text = word.explanation
        explanationView.caretPosition = 0

        tagsString = word.tags.joinToString(", ")
        tagsField.text = tagsString
    }

    private fun saveEditing() {
        if (!isModified) {
            return
        }

        val newWord = word.copy(
            phonetic = phoneticField.text,
            explanation = explanationView.text,
            tags = tagsField.text.toTagSet()
        )

        saveEditingButton.isEnabled = false
        val modalityState = ModalityState.current()
        val dialogRef = WeakReference(this)
        val expired = Condition<Any?> { dialogRef.get()?.isDisposed ?: true }
        runAsync { WordBookService.updateWord(newWord) }
            .onSuccess { updated ->
                if (updated) invokeLater(modalityState, expired) {
                    dialogRef.get()?.onEditingSaved(newWord)
                }
            }
            .onError { error ->
                invokeLater(modalityState, expired) {
                    dialogRef.get()?.onEditError(error)
                }
            }
            .onProcessed {
                invokeLater(modalityState, expired) {
                    dialogRef.get()?.saveEditingButton?.isEnabled = true
                }
            }
    }

    private fun onEditingSaved(newWord: WordBookItem) {
        setWord(newWord)
        IdeFocusManager.findInstance().requestFocus(closeButton, true)
    }

    private fun onEditError(error: Throwable) {
        val reason = when (error) {
            is WordBookException -> error.reason
            else -> {
                LOG.e("Failed to update word", error)
                "UNKNOWN_ERROR"
            }
        }
        val message = message("wordbook.notification.message.operation.failed", reason)
        Popups.showBalloonForComponent(saveEditingButton, message, MessageType.ERROR, project, Balloon.Position.above)
    }

    companion object {
        private const val TAG_SEPARATOR = ", "

        private val TRIM_CHARS = charArrayOf(',', '，', ' ', ' ' /* 0xA0 */)

        private val REGEX_WHITESPACE = Regex("[\\s ]+")

        private val LOG: Logger = Logger.getInstance(WordDetailsDialog::class.java)

        private fun Document.fixSeparator() {
            setText(text.replace(REGEX_WHITESPACE, " ").replace(REGEX_TAGS_SEPARATOR, TAG_SEPARATOR).trim(*TRIM_CHARS))
        }
    }
}