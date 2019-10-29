@file:Suppress("InvalidBundleOrProperty")

package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.FixedSizeCardLayout
import cn.yiiguxing.plugin.translate.ui.form.WordDialogForm
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.event.ActionEvent
import java.lang.Boolean.TRUE
import javax.swing.Action

/**
 * Word of the day dialog
 */
class WordOfTheDayDialog(project: Project?, words: List<WordBookItem>) : WordDialogForm(project) {

    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)
    private val layout = FixedSizeCardLayout()

    private var words: List<WordBookItem> = emptyList()
    private var currentWordIndex: Int = -1

    private val nextWordAction: Action = NextWordAction()
    private val previousWordAction: Action = PreviousWordAction()

    init {
        initViews()
        setWords(words)

        Disposer.register(disposable, ttsButton)
        init()
    }

    private fun initViews() {
        isModal = false
        title = message("word.of.the.day.title")
        horizontalStretch = 1.33f
        verticalStretch = 1.25f
        setCancelButtonText(message("word.dialog.close"))

        explainsCard.apply {
            removeAll()
            layout = this@WordOfTheDayDialog.layout
            add(maskPanel, CARD_MASK)
            add(
                explanationView,
                CARD_EXPLAINS_VIEW
            )
        }

        showExplanationButton.addActionListener {
            layout.show(
                explainsCard,
                CARD_EXPLAINS_VIEW
            )
        }
    }

    override fun createActions(): Array<Action> = arrayOf(previousWordAction, nextWordAction, cancelAction)

    fun setWords(words: List<WordBookItem>) {
        require(words.isNotEmpty()) { "Word list is empty" }
        if (words === this.words) {
            return
        }

        this.words = words
        this.currentWordIndex = -1
        next()
    }

    private fun previous() {
        check(currentWordIndex > 0) { "No more words in front" }
        setWord(words[--currentWordIndex])

        previousWordAction.isEnabled = (currentWordIndex - 1) >= 0
        nextWordAction.isEnabled = (currentWordIndex + 1) < words.size
    }

    private fun next() {
        check(currentWordIndex < words.size) { "No more words" }
        setWord(words[++currentWordIndex])

        previousWordAction.isEnabled = (currentWordIndex - 1) >= 0
        nextWordAction.isEnabled = (currentWordIndex + 1) < words.size
    }

    private fun setWord(word: WordBookItem) {
        wordView.text = word.word
        ttsButton.text = word.phonetic?.takeIf { it.isNotBlank() } ?: " "
        ttsButton.dataSource { word.word to word.sourceLanguage }
        explanationView.text = word.explanation
        explanationView.caretPosition = 0
        explanationLabel.text = message("word.language.explanation", word.targetLanguage.langName)

        layout.show(explainsCard, if (Settings.showExplanation) CARD_EXPLAINS_VIEW else CARD_MASK)
    }

    override fun show() {
        if (!isShowing) {
            super.show()
        }

        invokeLater { focusManager.requestFocus(window, true) }
    }

    private inner class PreviousWordAction : DialogWrapperAction(message("word.of.the.day.prev")) {
        override fun doAction(e: ActionEvent) = previous()
    }

    private inner class NextWordAction : DialogWrapperAction(message("word.of.the.day.next")) {
        init {
            putValue(DEFAULT_ACTION, TRUE)
            putValue(FOCUSED_ACTION, TRUE)
        }

        override fun doAction(e: ActionEvent) = next()
    }

    companion object {
        private const val CARD_MASK = "CARD_MASK"
        private const val CARD_EXPLAINS_VIEW = "CARD_EXPLAINS_VIEW"
    }
}