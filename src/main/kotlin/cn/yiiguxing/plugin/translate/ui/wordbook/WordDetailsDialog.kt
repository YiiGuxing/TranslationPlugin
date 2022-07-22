package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.Popups
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookException
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import cn.yiiguxing.plugin.translate.wordbook.WordBookView
import cn.yiiguxing.plugin.translate.wordbook.WordTags
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import org.jetbrains.concurrency.runAsync
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.lang.Boolean.TRUE
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentEvent as EditorDocumentEvent

/**
 * Word details dialog.
 */
class WordDetailsDialog(
    private val project: Project,
    private var word: WordBookItem
) : DialogWrapper(project) {

    private val tags: MutableSet<String> = TreeSet()

    private val tagCompletionProvider = TagsCompletionProvider(WordBookView.instance.wordTags) { tag ->
        synchronized(tags) { tag !in tags }
    }

    private val ui: WordDetailsUI = WordDetailsUI.Impl(project, tagCompletionProvider)

    private val saveAction = SaveAction()
    private val cancelAction = CancelAction()
    private val closeAction = CloseAction()


    init {
        isModal = true
        title = message("word.details.title")
        Disposer.register(disposable, ui)

        setupUi()
        init()
        setWord(word)
    }

    private val isModified: Boolean
        get() = ui.phoneticField.text != (word.phonetic ?: "")
                || (ui.explanationView.text ?: "") != (word.explanation ?: "")
                || tags != word.tags


    override fun createActions(): Array<Action> = arrayOf(saveAction, cancelAction, closeAction)

    override fun createCenterPanel(): JComponent = ui.contentComponent

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    private fun setupUi() {
        setupTagsField()
        ui.phoneticField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: EditorDocumentEvent) = checkModification()
        })
        ui.explanationView.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = checkModification()
        })
    }

    private fun setupTagsField() = with(ui.tagsField) {
        var toShowHint = true
        addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: EditorDocumentEvent) {
                updateTagSet(event.document.immutableCharSequence.toString())
                toShowHint = false
            }
        })

        addFocusListener(object : FocusListener {

            override fun focusLost(e: FocusEvent) {
                updateTagSet(document.text)
                text = WordTags.getTagsString(tags)
            }

            override fun focusGained(e: FocusEvent) {
                if (toShowHint && document.textLength == 0) {
                    invokeLater(expired = { editor.let { it == null || it.isDisposed } }) {
                        editor?.let {
                            HintManager.getInstance().showInformationHint(it, message("word.details.tags.hit"))
                        }
                    }
                }
            }
        })
    }

    private fun updateTagSet(tagsString: String) {
        synchronized(tags) {
            tags.clear()
            if (tagsString.isNotEmpty()) {
                WordTags.toTagSet(tagsString, tags)
            }
        }

        checkModification()
    }

    private fun checkModification() {
        isModified.let { modified ->
            getButton(saveAction)?.isVisible = modified
            getButton(cancelAction)?.isVisible = modified
            rootPane.defaultButton = getButton(if (modified) saveAction else closeAction)
        }
    }

    private fun setWord(word: WordBookItem) {
        this.word = word
        with(ui) {
            wordView.text = word.word
            languageLabel.text = word.sourceLanguage.langName
            ttsButton.dataSource { word.word to word.sourceLanguage }
            phoneticField.text = word.phonetic ?: ""
            explanationLabel.text = message("word.language.explanation", word.targetLanguage.langName)
            explanationView.text = word.explanation
            explanationView.caretPosition = 0
            tagsField.text = WordTags.getTagsString(word.tags)
        }
    }

    private fun saveEditing() {
        if (!isModified) {
            return
        }

        val newWord = word.copy(
            phonetic = ui.phoneticField.text,
            explanation = ui.explanationView.text,
            tags = TreeSet(tags)
        )

        saveAction.isEnabled = false
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
                    dialogRef.get()?.saveAction?.isEnabled = true
                }
            }
    }

    private fun onEditingSaved(newWord: WordBookItem) {
        setWord(newWord)
        tagCompletionProvider.appendTags(newWord.tags)
        requestActionFocus(closeAction)
    }

    private fun onEditError(error: Throwable) {
        val reason = when (error) {
            is WordBookException -> error.errorCode.reason
            else -> {
                LOG.e("Failed to update word", error)
                "UNKNOWN_ERROR"
            }
        }

        getButton(closeAction)?.let {
            val message = message("wordbook.notification.message.operation.failed", reason)
            Popups.showBalloonForComponent(it, message, MessageType.ERROR, project, Balloon.Position.above)
        }
    }

    private fun cancelEditing() {
        ui.phoneticField.text = word.phonetic ?: ""
        ui.explanationView.text = word.explanation
        ui.tagsField.text = WordTags.getTagsString(word.tags)

        requestActionFocus(closeAction)
    }

    private fun requestActionFocus(action: Action) {
        getButton(action)?.let { IdeFocusManager.findInstance().requestFocus(it, true) }
    }

    private inner class CloseAction : DialogWrapper.DialogWrapperAction(message("close.action.name")) {
        init {
            putValue(DEFAULT_ACTION, TRUE)
            putValue(FOCUSED_ACTION, TRUE)
        }

        override fun doAction(e: ActionEvent) = close(OK_EXIT_CODE)
    }

    private inner class SaveAction : DialogWrapper.DialogWrapperAction(message("word.details.action.edit.save")) {
        override fun doAction(e: ActionEvent) = saveEditing()
    }

    private inner class CancelAction : DialogWrapper.DialogWrapperAction(message("word.details.action.edit.cancel")) {
        override fun doAction(e: ActionEvent) = cancelEditing()
    }

    companion object {
        private val LOG: Logger = Logger.getInstance(WordDetailsDialog::class.java)
    }
}