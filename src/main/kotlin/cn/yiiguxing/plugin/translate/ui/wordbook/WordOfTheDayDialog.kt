package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.TTSButton
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.Viewer
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import java.awt.CardLayout
import java.awt.event.ActionEvent
import java.lang.Boolean.TRUE
import javax.swing.*

/**
 * Word of the day dialog
 */
class WordOfTheDayDialog(project: Project?, words: List<WordBookItem>) : DialogWrapper(project) {

    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)

    private var words: List<WordBookItem> = emptyList()
    private var currentWordIndex: Int = -1

    private val wordView: Viewer = Viewer()
    private val ttsButton: TTSButton = TTSButton()
    private val explanationLayout = CardLayout()
    private val explainsCard: JPanel = JPanel(explanationLayout)
    private val explanationLabel: JBLabel = JBLabel()
    private val explanationView: Viewer = Viewer()
    private val maskPanel: JPanel = JPanel(VerticalLayout(0, SwingConstants.CENTER))
    private val showExplanationButton: JButton = JButton(message("word.of.the.day.show.explanation"))

    private val nextWordAction: Action = NextWordAction()
    private val previousWordAction: Action = PreviousWordAction()

    init {
        isModal = false
        title = message("word.of.the.day.title")
        setCancelButtonText(message("word.dialog.close"))
        Disposer.register(disposable, ttsButton)

        init()
        setWords(words)
    }

    override fun createCenterPanel() = JPanel(UI.migLayout()).apply {
        border = JBUI.Borders.empty(16)
        background = UIManager.getColor("TextArea.background")

        JBDimension(550, 400).let {
            minimumSize = it
            preferredSize = it
        }

        val (primaryFont, phoneticFont) = UI.getFonts(15, 14)
        wordView.apply {
            border = JBUI.Borders.emptyBottom(16)
            font = primaryFont.biggerOn(5f).asBold()
        }
        add(wordView, UI.fillX().wrap())

        ttsButton.font = phoneticFont
        add(ttsButton, UI.wrap())

        explanationLabel.border = JBUI.Borders.empty(8, 0, 4, 0)
        add(explanationLabel, UI.wrap())

        maskPanel.add(showExplanationButton, VerticalLayout.CENTER)
        explanationView.apply {
            font = primaryFont
            margin = JBUI.insets(6)
        }
        explainsCard.apply {
            border = JBUI.Borders.customLine(UI.getBorderColor(), 1)
            add(maskPanel, CARD_MASK)
            val scrollPane = JBScrollPane(explanationView).apply {
                border = JBUI.Borders.empty()
            }
            add(scrollPane, CARD_EXPLAINS_VIEW)
        }
        showExplanationButton.addActionListener {
            explanationLayout.show(explainsCard, CARD_EXPLAINS_VIEW)
        }
        add(explainsCard, UI.fill())
    }

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

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

        explanationLayout.show(explainsCard, if (Settings.showExplanation) CARD_EXPLAINS_VIEW else CARD_MASK)
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