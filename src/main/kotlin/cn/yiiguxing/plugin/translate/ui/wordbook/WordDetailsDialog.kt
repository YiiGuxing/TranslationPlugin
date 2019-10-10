package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordDetailsDialogForm
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import javax.swing.Action
import javax.swing.event.DocumentEvent
import javax.swing.text.AttributeSet
import javax.swing.text.DefaultStyledDocument

/**
 * Word details dialog.
 *
 * Created by Yii.Guxing on 2019/09/06.
 */
class WordDetailsDialog(private var word: WordBookItem) : WordDetailsDialogForm() {


    init {
        isModal = true
        title = message("word.details.title")
        horizontalStretch = 1.33f
        verticalStretch = 1.25f
        Disposer.register(disposable, ttsButton)

        setWord(word)
        initActions()
        init()
    }

    private val isModified: Boolean
        get() = (phoneticField.text ?: "") != (word.phonetic ?: "") ||
                (explanationView.text ?: "") != (word.explanation ?: "")

    override fun createActions(): Array<Action> = emptyArray()

    private fun initActions() {
        closeButton.addActionListener { close(OK_EXIT_CODE) }
        saveEditingButton.addActionListener { saveEditing() }
        cancelEditingButton.addActionListener {
            phoneticField.text = word.phonetic
            explanationView.text = word.explanation
        }

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = checkModification()
        }
        phoneticField.document.addDocumentListener(listener)
        explanationView.document.addDocumentListener(listener)
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
    }

    private fun saveEditing() {
        if (isModified) {
            val newWord = word.copy(phonetic = phoneticField.text, explanation = explanationView.text)
            if (WordBookService.updateWord(newWord)) {
                setWord(newWord)
            }
        }
    }

    private class TagsDocument : DefaultStyledDocument() {

        override fun insertString(offset: Int, string: String?, attr: AttributeSet?) {
            var insertText: String = string ?: return
            if (length == 0) {
                insertText = insertText.trim()
            } else {
                if (offset == 0 || getText(offset - 1, 1)[0].isWhitespace()) {
                    insertText = insertText.trimStart()
                }
                if (offset != length && getText(offset, 1)[0].isWhitespace()) {
                    insertText = insertText.trimEnd()
                }
            }

            insertText = insertText.replace(",", "").replace(REGEX_WHITESPACE, " ")
            if (insertText.isEmpty()) return

            super.insertString(offset, insertText, attr)
        }

        override fun remove(offset: Int, len: Int) {
            var removeLength = len
            if (offset == 0 && removeLength < length && getText(offset + removeLength, 1)[0].isWhitespace()) {
                removeLength++
            }

            super.remove(offset, removeLength)
        }

    }

    companion object {
        private val REGEX_WHITESPACE = "\\s+".toRegex()
    }

}