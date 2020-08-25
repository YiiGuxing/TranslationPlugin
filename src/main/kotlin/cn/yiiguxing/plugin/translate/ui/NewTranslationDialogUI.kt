package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.UI.fill
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.lineAbove
import cn.yiiguxing.plugin.translate.ui.UI.lineBelow
import cn.yiiguxing.plugin.translate.ui.UI.lineToRight
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.plus
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.ui.icon.LangComboBoxLink
import com.intellij.icons.AllIcons
import com.intellij.ui.PopupBorder
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

interface NewTranslationDialogUI {
    val topPanel: JPanel
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
    val srcTransliterationLabel: JLabel
    val targetTransliterationLabel: JLabel
    val pinButton: JComponent
    val settingsButton: JComponent
    val dictViewerCollapsible: CollapsiblePanel
    val dictViewer: StyledViewer

    fun createMainPanel(): JPanel

    fun initFonts(pair: UI.FontPair)
}

class NewTranslationDialogUiImpl(uiProvider: NewTranslationDialogUiProvider) : NewTranslationDialogUI {
    private val mRoot: JPanel = JPanel()
    override val topPanel: JPanel = JPanel()

    override val sourceLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val targetLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val swapButton: LinkLabel<Void> = LinkLabel()
    override val inputTextArea: JTextArea = JBTextArea(1, 1)
    override val translationTextArea: JTextArea = JBTextArea(1, 1)

    override val inputTTSButton: TTSButton = TTSButton()
    override val translationTTSButton: TTSButton = TTSButton()
    override val clearButton: LinkLabel<Void> = LinkLabel()
    override val copyButton: LinkLabel<Void> = LinkLabel()
    override val starButton: LinkLabel<Translation> = LinkLabel()
    override val historyButton: LinkLabel<Void> = LinkLabel()
    override val detectedLanguageLabel: JLabel = JLabel()
    override val srcTransliterationLabel: JLabel = TransliterationLabel(20)
    override val targetTransliterationLabel: JLabel = TransliterationLabel(20)

    override val pinButton: JComponent = uiProvider.createPinButton()
    override val settingsButton: JComponent = uiProvider.createSettingsButton()

    override val dictViewer: StyledViewer = StyledViewer()
    override val dictViewerCollapsible: CollapsiblePanel =
        CollapsiblePanel(dictViewer, message("translation.dialog.more.translations"))

    override fun createMainPanel(): JPanel {
        layoutMainPanel()
        layoutTopPanel()
        layoutLangComboBoxes()
        initTextAreas()
        setButtonIcons()

        return mRoot
    }

    override fun initFonts(pair: UI.FontPair) {
        pair.let { (primaryFont, phoneticFont) ->
            val labelsFont = primaryFont.deriveFont(JBFont.label().size.toFloat())
            sourceLangComboBox.font = labelsFont
            targetLangComboBox.font = labelsFont
            detectedLanguageLabel.font = labelsFont
            inputTextArea.font = primaryFont
            translationTextArea.font = primaryFont
            srcTransliterationLabel.font = phoneticFont
            targetTransliterationLabel.font = phoneticFont
        }
    }

    private fun layoutLangComboBoxes() {
        fun update(comboBox: LangComboBoxLink) {
            comboBox.background = topPanel.background
            comboBox.border = JBUI.Borders.empty(4, 10, 4, 0)
        }

        update(sourceLangComboBox)
        update(targetLangComboBox)
    }

    private fun initTextAreas() {
        fun init(textArea: JTextArea) {
            textArea.apply {
                lineWrap = true
                wrapStyleWord = true
                border = JBUI.Borders.empty(10, 10, 0, 10)
                minimumSize = JBDimension(0, textArea.getFontMetrics(textArea.font).height + 10)
            }
        }
        init(inputTextArea)
        init(translationTextArea)
    }

    private fun layoutMainPanel(): JPanel {
        mRoot.apply {
            layout = migLayout()
            border = PopupBorder.Factory.create(true, true)

            add(topPanel, fillX().span(2).wrap())

            val leftPanel = JPanel(migLayout()).apply {
                add(inputTextArea, fill().wrap())
                add(createToolbar(clearButton, historyButton), fillX())
                border = lineToRight()
                background = inputTextArea.background
            }
            val rightPanel = JPanel(migLayout()).apply {
                add(translationTextArea, fill().wrap())
                add(createToolbar(copyButton, starButton), fillX())
                background = translationTextArea.background
            }
            add(leftPanel, fill().sizeGroup("content"))
            add(rightPanel, fill().sizeGroup("content").wrap())

            val transliterations = JPanel(migLayout()).apply {
                add(createTransliterationPanel(inputTTSButton, srcTransliterationLabel), fill().sizeGroup("half"))
                add(createTransliterationPanel(translationTTSButton, targetTransliterationLabel), fill().sizeGroup("half").wrap())
            }

            add(transliterations, fillX().span(2).wrap())

            val dictViewerPanel = dictViewerCollapsible.panel.apply { border = emptyBorder(6, 10) + lineAbove() }
            add(dictViewerPanel, fillX().span(2))
        }

        return mRoot
    }

    private fun layoutTopPanel() {
        val left = JPanel(HorizontalLayout(10)).apply {
            add(sourceLangComboBox, HorizontalLayout.LEFT)
            add(detectedLanguageLabel, HorizontalLayout.LEFT)

            detectedLanguageLabel.isEnabled = false
            detectedLanguageLabel.minimumSize = Dimension(0, 0)
        }

        val right = JPanel(HorizontalLayout(3)).apply {
            add(targetLangComboBox, HorizontalLayout.LEFT)
            add(settingsButton, HorizontalLayout.RIGHT)
            add(Separator(), HorizontalLayout.RIGHT)
            add(pinButton, HorizontalLayout.RIGHT)
        }

        val halfTopPanel = "halfTopPanel"

        topPanel.apply {
            layout = migLayout()
            border = emptyBorder(6) + lineBelow()
            size = JBDimension(700, topPanel.preferredSize.height)
            add(left, fillX().sizeGroup(halfTopPanel))
            add(swapButton)
            add(right, fillX().sizeGroup(halfTopPanel))
        }
    }

    private fun createToolbar(vararg buttons: JComponent): JPanel {
        return JPanel(HorizontalLayout(10)).apply {
            buttons.iterator().forEach {
                add(it, HorizontalLayout.RIGHT)
            }
            border = emptyBorder(6, 10)
            background = inputTextArea.background
        }
    }

    private fun createTransliterationPanel(button: TTSButton, label: JLabel): JPanel {
        return JPanel(HorizontalLayout(10)).apply {
            add(button, HorizontalLayout.LEFT)
            add(label, HorizontalLayout.LEFT)
            border = emptyBorder(6, 10) + lineAbove()
        }
    }

    private fun setButtonIcons() {
        swapButton.setIcons(Icons.Swap)
        copyButton.setIcons(AllIcons.Actions.Copy)
        clearButton.setIcons(AllIcons.Actions.GC)
        historyButton.setIcons(AllIcons.Vcs.History)
        starButton.setIcons(Icons.GrayStarOff)
    }

    private class Separator : JComponent() {
        val myColor = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
        val myHGap = 2
        val myVGap = 4

        override fun paintComponent(g: Graphics) {
            g.color = myColor
            g.drawLine(myHGap, myVGap, myHGap, height - myVGap - 1)
        }

        override fun getPreferredSize(): Dimension {
            return Dimension(2 * myHGap + 1, myVGap * 2 + 1)
        }

        override fun getMinimumSize(): Dimension? {
            return preferredSize
        }

        override fun getMaximumSize(): Dimension? {
            return preferredSize
        }
    }
}