package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.util.ScrollSynchronizer
import com.intellij.openapi.Disposable
import com.intellij.ui.components.labels.LinkLabel
import javax.swing.*

interface TranslationDialogUI : Disposable {
    val topPanel: JPanel
    val translationPanel: JPanel
    val sourceLangComboBox: LangComboBoxLink
    val targetLangComboBox: LangComboBoxLink
    val swapButton: LinkLabel<Void>
    val inputTextArea: JTextArea
    val translationTextArea: JTextArea
    val inputTTSButton: TTSButton
    val translationTTSButton: TTSButton
    val clearButton: LinkLabel<Void>
    val copyButton: LinkLabel<Void>
    val starButton: LinkLabel<Translation>
    val historyButton: LinkLabel<Void>
    val detectedLanguageLabel: JLabel
    val lightningLabel: JLabel
    val srcTransliterationLabel: JLabel
    val targetTransliterationLabel: JLabel
    val pinButton: JComponent
    val settingsButton: JComponent
    val dictViewer: StyledViewer
    val dictViewerPanel: JScrollPane
    val spellComponent: SpellComponent
    val fixLangComponent: FixLangComponent

    val expandDictViewerButton: LinkLabel<Void>
    val collapseDictViewerButton: LinkLabel<Void>

    val translationFailedComponent: TranslationFailedComponent

    val scrollSynchronizer: ScrollSynchronizer

    fun createMainPanel(): JComponent

    fun initFonts(pair: UI.FontPair)

    fun showProgress()
    fun hideProgress()

    fun showTranslationPanel()
    fun expandDictViewer()
    fun collapseDictViewer()
    fun hideDictViewer()

    fun showErrorPanel()
}