package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.UI.fill
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.lineAbove
import cn.yiiguxing.plugin.translate.ui.UI.lineBelow
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.migLayoutVertical
import cn.yiiguxing.plugin.translate.ui.UI.plus
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.ui.util.ScrollSynchronizer
import cn.yiiguxing.plugin.translate.util.alphaBlend
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.ui.PopupBorder
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil.getLineHeight
import icons.Icons
import net.miginfocom.layout.CC
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.lang.Integer.min
import javax.swing.*
import javax.swing.text.JTextComponent

interface NewTranslationDialogUI {
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

    val scrollSynchronizer: ScrollSynchronizer

    fun createMainPanel(): JComponent

    fun setActive(active: Boolean)

    fun initFonts(pair: UI.FontPair)

    fun expandDictViewer()
    fun collapseDictViewer()
    fun hideDictViewer()
}

class NewTranslationDialogUiImpl(uiProvider: NewTranslationDialogUiProvider) : NewTranslationDialogUI {
    private val mRoot: JPanel = JPanel()
    override val topPanel: JPanel = JPanel()
    private val bottomPanel: JPanel = JPanel()
    override lateinit var translationPanel: JPanel

    override val sourceLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val targetLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val swapButton: LinkLabel<Void> = LinkLabel()
    override val inputTextArea: JTextArea = JBTextArea(1, 1)
    override val translationTextArea: JTextArea = JBTextArea(1, 1).apply {
        isEditable = false
        background = topPanel.background
    }

    private val inputTextAreaWrapper = createScrollPane(inputTextArea, ScrollPane.FADING_END)
    private val translationTextAreaWrapper = createScrollPane(translationTextArea, ScrollPane.FADING_END)

    override val scrollSynchronizer: ScrollSynchronizer = ScrollSynchronizer.syncScroll(
        inputTextAreaWrapper.verticalScrollBar,
        translationTextAreaWrapper.verticalScrollBar
    )

    override val inputTTSButton: TTSButton = TTSButton()
    override val translationTTSButton: TTSButton = TTSButton()
    override val clearButton: LinkLabel<Void> = LinkLabel()
    override val copyButton: LinkLabel<Void> = LinkLabel()
    override val starButton: LinkLabel<Translation> = LinkLabel()
    override val historyButton: LinkLabel<Void> = LinkLabel()
    override val detectedLanguageLabel: JLabel = JLabel()
    override val lightningLabel: JLabel = JLabel().apply {
        isOpaque = false
        icon = Icons.Lightning
    }
    override val srcTransliterationLabel: JLabel = TransliterationLabel()
    override val targetTransliterationLabel: JLabel = TransliterationLabel()

    override val pinButton: JComponent = uiProvider.createPinButton()
    override val settingsButton: JComponent = uiProvider.createSettingsButton()

    override val dictViewer: StyledViewer = StyledViewer().apply {
        isOpaque = true
        background = JBColor {
            JBUI.CurrentTheme.CustomFrameDecorations.paneBackground().alphaBlend(inputTextArea.background, 0.65f)
        }
    }
    override val dictViewerPanel: JScrollPane = createScrollPane(dictViewer)

    override val expandDictViewerButton: LinkLabel<Void> = LinkLabel()
    override val collapseDictViewerButton: LinkLabel<Void> = LinkLabel()

    override val spellComponent: SpellComponent = createSpellComponent()
    override val fixLangComponent: FixLangComponent = FixLangComponent()

    override fun createMainPanel(): JComponent {
        initTextAreas()
        initDictViewer()
        layoutTopPanel()
        layoutMainPanel()
        setButtonIcons()

        return mRoot
    }

    override fun setActive(active: Boolean) {
        mRoot.border = PopupBorder.Factory.create(active, true)
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
            dictViewer.font = primaryFont
        }
    }

    private fun initTextAreas() {
        fun init(textArea: JTextArea) {
            textArea.apply {
                lineWrap = true
                wrapStyleWord = true
                border = emptyBorder(10)
                minimumSize = JBDimension(0, minHeight(textArea))
            }
        }
        init(inputTextArea)
        init(translationTextArea)
    }

    override fun expandDictViewer() {
        expandDictViewerButton.isVisible = false
        collapseDictViewerButton.isVisible = true
        dictViewerPanel.isVisible = true
    }

    override fun collapseDictViewer() {
        expandDictViewerButton.isVisible = true
        collapseDictViewerButton.isVisible = false
        dictViewerPanel.isVisible = false
    }

    override fun hideDictViewer() {
        expandDictViewerButton.isVisible = false
        collapseDictViewerButton.isVisible = false
        dictViewerPanel.isVisible = false
    }

    private fun maxDictViewerHeight() = 20 * getLineHeight(dictViewer)

    private fun initDictViewer() {
        dictViewer.apply {
            border = emptyBorder(10)
            minimumSize = Dimension(0, 0)
        }
        dictViewerPanel.apply {
            maximumSize = Dimension(Int.MAX_VALUE, maxDictViewerHeight())
            border = lineAbove()
        }

        expandDictViewerButton.apply {
            setIcons(Icons.ArrowDownExpand)
            text = message("translation.dialog.more.translations")
            horizontalTextPosition = SwingConstants.LEADING
            foreground = JBUI.CurrentTheme.Label.foreground()
            setPaintUnderline(false)
            setListener({ _, _ -> expandDictViewer() }, null)
        }
        collapseDictViewerButton.apply {
            setIcons(Icons.ArrowUpCollapse)
            setListener({ _, _ -> collapseDictViewer() }, null)
        }
    }

    private fun layoutMainPanel(): JComponent {

        val centerPanel = JPanel(migLayout()).apply {
            val leftPanel = JPanel(migLayout()).apply {
                background = inputTextArea.background

                add(inputTextAreaWrapper, fill().wrap())
                add(createToolbar(inputTTSButton, srcTransliterationLabel, clearButton, historyButton), fillX())
            }
            val rightPanel = JPanel(migLayout()).apply {
                background = translationTextArea.background

                add(translationTextAreaWrapper, fill().wrap())
                add(createToolbar(translationTTSButton, targetTransliterationLabel, copyButton, starButton), fillX())
            }

            add(leftPanel, fill().sizeGroup("content"))
            add(rightPanel, fill().sizeGroup("content").wrap())
        }

        bottomPanel.apply {
            layout = HorizontalLayout(JBUIScale.scale(10))
            border = emptyBorder(6, 10) + lineAbove()

            add(spellComponent, HorizontalLayout.LEFT)
            add(fixLangComponent, HorizontalLayout.LEFT)
            add(JLabel(" "), HorizontalLayout.LEFT)
            add(expandDictViewerButton, HorizontalLayout.RIGHT)
            add(collapseDictViewerButton, HorizontalLayout.RIGHT)
        }
        translationPanel = JPanel(migLayoutVertical()).apply {
            add(topPanel, fillX())
            add(centerPanel, fill())
            add(bottomPanel, fillX())
        }

        mRoot.apply {
            border = PopupBorder.Factory.create(true, true)
            layout = BoxLayout(mRoot, BoxLayout.Y_AXIS)

            add(translationPanel)
            add(dictViewerPanel)
        }

        return mRoot
    }

    private fun layoutTopPanel() {
        val left = JPanel(HorizontalLayout(10)).apply {
            border = emptyBorder(0, leftAndRight = 5)

            add(sourceLangComboBox, HorizontalLayout.LEFT)
            add(detectedLanguageLabel, HorizontalLayout.LEFT)

            detectedLanguageLabel.isEnabled = false
            detectedLanguageLabel.minimumSize = Dimension(0, 0)
        }

        val right = JPanel(HorizontalLayout(3)).apply {
            border = emptyBorder(0, leftAndRight = 5)

            add(targetLangComboBox, HorizontalLayout.LEFT)
            add(lightningLabel, HorizontalLayout.LEFT)
            add(settingsButton, HorizontalLayout.RIGHT)
            add(Separator(), HorizontalLayout.RIGHT)
            add(pinButton, HorizontalLayout.RIGHT)
        }

        val halfTopPanel = "halfTopPanel"

        topPanel.apply {
            layout = migLayout()
            border = emptyBorder(topAndBottom = 6, leftAndRight = 0) + lineBelow()
            add(left, fillX().sizeGroup(halfTopPanel))
            add(swapButton)
            add(right, fillX().sizeGroup(halfTopPanel))
        }

        sourceLangComboBox.background = topPanel.background
        targetLangComboBox.background = topPanel.background
    }

    private fun createToolbar(ttsButton: TTSButton, transliterationLabel: JLabel, vararg buttons: JComponent): JPanel {
        return NonOpaquePanel(migLayout()).apply {
            add(ttsButton)

            add(NonOpaquePanel(VerticalFlowLayout(0, 0)).apply {
                add(transliterationLabel)
                minimumSize = Dimension(0, 0)
            }, fillX().alignY("center").gapLeft("${JBUIScale.scale(5)}px"))

            buttons.iterator().forEach {
                add(it, CC().gapLeft("${JBUIScale.scale(8)}px"))
            }

            border = emptyBorder(6, 10)
        }
    }

    private fun createSpellComponent(): SpellComponent = SpellComponent().apply {
        spellText.apply {
            font = font.deriveFont(Font.BOLD, spellLabel.font.size.toFloat())
        }
    }

    private fun setButtonIcons() {
        swapButton.setIcons(Icons.Swap)
        copyButton.setIcons(AllIcons.Actions.Copy)
        clearButton.setIcons(AllIcons.Actions.GC)
        historyButton.setIcons(AllIcons.Vcs.History)
        starButton.setIcons(Icons.GrayStarOff)
    }

    private fun createScrollPane(component: JComponent, fadingFlag: Int = ScrollPane.FADING_ALL): JScrollPane =
        object : ScrollPane(component) {
            init {
                border = emptyBorder(0)
            }

            override fun getFadingEdgeColor(): Color = component.background

            override fun getFadingEdgeSize(): Int = 10

            override fun getFadingFlag(): Int = fadingFlag

            override fun getMinimumSize(): Dimension {
                // avoid scrollbar around minimum size
                val componentMinSize = component.minimumSize
                return Dimension(componentMinSize.width, componentMinSize.height + 5)
            }

            override fun getPreferredSize(): Dimension {
                val preferred = super.getPreferredSize()
                val max = maximumSize
                return Dimension(min(preferred.width, max.width), min(preferred.height, max.height))
            }

        }

    private fun minHeight(textComponent: JTextComponent): Int {
        val borderInsets = textComponent.border.getBorderInsets(textComponent)
        return getLineHeight(textComponent) + borderInsets.top + borderInsets.bottom
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

        override fun getMinimumSize(): Dimension {
            return preferredSize
        }

        override fun getMaximumSize(): Dimension {
            return preferredSize
        }
    }
}