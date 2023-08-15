package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.compat.ui.GotItTooltipPosition
import cn.yiiguxing.plugin.translate.compat.ui.show
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.UI.plus
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.ui.icon.SmallProgressIcon
import cn.yiiguxing.plugin.translate.ui.util.ScrollSynchronizer
import cn.yiiguxing.plugin.translate.util.alphaBlend
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.*
import icons.TranslationIcons
import net.miginfocom.layout.CC
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import javax.swing.*
import javax.swing.text.JTextComponent

class TranslationDialogUiImpl(uiProvider: TranslationDialogUiProvider) : TranslationDialogUI {
    private val mRoot: JPanel = JPanel()
    override val topPanel: JPanel = JPanel()
    private val bottomPanel: JPanel = JPanel()
    override lateinit var translationPanel: JPanel

    override val sourceLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val targetLangComboBox: LangComboBoxLink = LangComboBoxLink()
    override val swapButton: LinkLabel<Void> = LinkLabel()
    override val inputTextArea: JTextArea = JBTextArea(1, 1)
    override val translationTextArea: JTextArea = TranslationTextArea(topPanel.background)

    override val translationFailedComponent: TranslationFailedComponent = TranslationFailedComponent()
    private val rightPanelLayout = FixedSizeCardLayout()
    private val rightPanel: JPanel = JPanel(rightPanelLayout).apply {
        background = topPanel.background
        add(translationTextArea, CARD_TRANSLATION)
        add(translationFailedComponent, CARD_ERROR)
        rightPanelLayout.show(this, CARD_TRANSLATION)
    }

    private val inputTextAreaWrapper = createScrollPane(inputTextArea, ScrollPane.FADING_END)
    private val rightPanelWrapper = createScrollPane(rightPanel, ScrollPane.FADING_END)

    override val scrollSynchronizer: ScrollSynchronizer = ScrollSynchronizer.syncScroll(
        inputTextAreaWrapper.verticalScrollBar,
        rightPanelWrapper.verticalScrollBar
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
        icon = TranslationIcons.Lightning
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
    private val progressIcon: AnimatedIcon = SmallProgressIcon().apply {
        isVisible = false
        suspend()
    }

    override fun createMainPanel(): JComponent {
        initTextAreas()
        initDictViewer()
        layoutTopPanel()
        layoutMainPanel()
        setButtonIcons()
        setupTooltips()

        return mRoot
    }

    override fun initFonts(pair: UI.FontPair) {
        pair.let { (primaryFont, phoneticFont) ->
            val labelFont = primaryFont.deriveFont(JBFont.label().size.toFloat())
            sourceLangComboBox.font = labelFont
            targetLangComboBox.font = labelFont
            detectedLanguageLabel.font = labelFont
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
                border = UI.emptyBorder(10)
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

    private fun maxDictViewerHeight() = 20 * UIUtil.getLineHeight(dictViewer)

    private fun initDictViewer() {
        dictViewer.apply {
            border = UI.emptyBorder(10)
            minimumSize = Dimension(0, 0)
        }
        dictViewerPanel.apply {
            maximumSize = Dimension(Int.MAX_VALUE, maxDictViewerHeight())
            border = UI.lineAbove()
        }

        expandDictViewerButton.apply {
            setIcons(TranslationIcons.ArrowDownExpand)
            text = message("translation.dialog.more.translations")
            horizontalTextPosition = SwingConstants.LEADING
            foreground = JBUI.CurrentTheme.Label.foreground()
            setPaintUnderline(false)
            setListener({ _, _ -> expandDictViewer() }, null)
        }
        collapseDictViewerButton.apply {
            setIcons(TranslationIcons.ArrowUpCollapse)
            setListener({ _, _ -> collapseDictViewer() }, null)
        }
    }

    private fun layoutMainPanel(): JComponent {
        val centerPanel = JPanel(UI.migLayout()).apply {
            val leftPanel = JPanel(UI.migLayout()).apply {
                background = inputTextArea.background

                add(inputTextAreaWrapper, UI.fill().wrap())
                add(createToolbar(inputTTSButton, srcTransliterationLabel, clearButton, historyButton), UI.fillX())
            }
            val rightPanel = JPanel(UI.migLayout()).apply {
                background = translationTextArea.background
                val borderColor = JBUI.CurrentTheme.Popup.borderColor(false)
                border = JBUI.Borders.customLine(borderColor, 0, 1, 0, 0)

                add(rightPanelWrapper, UI.fill().wrap())
                add(
                    createToolbar(translationTTSButton, targetTransliterationLabel, copyButton, starButton),
                    UI.fillX().cell(0, 1)
                )
            }

            add(leftPanel, UI.fill().sizeGroup("content"))
            add(rightPanel, UI.fill().sizeGroup("content").wrap())
        }

        bottomPanel.apply {
            layout = HorizontalLayout(JBUIScale.scale(10))
            border = UI.emptyBorder(6, 10) + UI.lineAbove()

            add(spellComponent, HorizontalLayout.LEFT)
            add(fixLangComponent, HorizontalLayout.LEFT)
            add(progressIcon, HorizontalLayout.LEFT)
            add(JLabel(" "), HorizontalLayout.LEFT)
            add(expandDictViewerButton, HorizontalLayout.RIGHT)
            add(collapseDictViewerButton, HorizontalLayout.RIGHT)
        }
        translationPanel = JPanel(UI.migLayoutVertical()).apply {
            add(topPanel, UI.fillX())
            add(centerPanel, UI.fill())
            add(bottomPanel, UI.fillX())
        }

        mRoot.apply {
            layout = BoxLayout(mRoot, BoxLayout.Y_AXIS)

            add(translationPanel)
            add(dictViewerPanel)
        }

        return mRoot
    }

    private fun layoutTopPanel() {
        val left = JPanel(HorizontalLayout(10)).apply {
            border = UI.emptyBorder(0, leftAndRight = 5)

            add(sourceLangComboBox, HorizontalLayout.LEFT)
            add(detectedLanguageLabel, HorizontalLayout.LEFT)

            detectedLanguageLabel.isEnabled = false
            detectedLanguageLabel.minimumSize = Dimension(0, 0)
        }

        val right = JPanel(HorizontalLayout(3)).apply {
            border = UI.emptyBorder(0, leftAndRight = 5)

            add(targetLangComboBox, HorizontalLayout.LEFT)
            add(lightningLabel, HorizontalLayout.LEFT)
            add(settingsButton, HorizontalLayout.RIGHT)
            add(Separator(), HorizontalLayout.RIGHT)
            add(pinButton, HorizontalLayout.RIGHT)
        }

        val halfTopPanel = "halfTopPanel"

        topPanel.apply {
            layout = UI.migLayout()
            border = UI.emptyBorder(topAndBottom = 6, leftAndRight = 0) + UI.lineBelow()
            add(left, UI.fillX().sizeGroup(halfTopPanel))
            add(swapButton)
            add(right, UI.fillX().sizeGroup(halfTopPanel))
        }

        sourceLangComboBox.background = topPanel.background
        targetLangComboBox.background = topPanel.background
    }

    private fun createToolbar(ttsButton: TTSButton, transliterationLabel: JLabel, vararg buttons: JComponent): JPanel {
        return NonOpaquePanel(UI.migLayout()).apply {
            add(ttsButton)

            add(NonOpaquePanel(VerticalFlowLayout(0, 0)).apply {
                add(transliterationLabel)
                minimumSize = Dimension(0, 0)
            }, UI.fillX().alignY("center").gapLeft("${JBUIScale.scale(5)}px"))

            buttons.iterator().forEach {
                add(it, CC().gapLeft("${JBUIScale.scale(8)}px"))
            }

            border = UI.emptyBorder(6, 10)
        }
    }

    private fun createSpellComponent(): SpellComponent = SpellComponent().apply {
        preferredSize = JBDimension(350, -1)
        spellText.apply {
            font = font.deriveFont(Font.BOLD, spellLabel.font.size.toFloat())
        }
    }

    private fun setButtonIcons() {
        swapButton.setIcons(TranslationIcons.Swap)
        copyButton.setIcons(AllIcons.Actions.Copy)
        clearButton.setIcons(AllIcons.Actions.GC)
        historyButton.setIcons(AllIcons.Vcs.History)
        starButton.setIcons(TranslationIcons.GrayStarOff)
    }

    private fun setupTooltips() {
        val id = "${TranslationPlugin.PLUGIN_ID}.tooltip.new.translation.engines.openai"
        val message = message("got.it.tooltip.text.new.translation.engines")
        GotItTooltip(id, message, this)
            .withHeader(message("got.it.tooltip.title.new.translation.engines"))
            .show(settingsButton, GotItTooltipPosition.BOTTOM)
    }

    private fun createScrollPane(component: JComponent, fadingFlag: Int = ScrollPane.FADING_ALL): JScrollPane =
        object : ScrollPane(component) {
            init {
                border = UI.emptyBorder(0)
            }

            override fun getFadingEdgeColor(): Color? = component.background

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
                return Dimension(Integer.min(preferred.width, max.width), Integer.min(preferred.height, max.height))
            }

        }

    private fun minHeight(textComponent: JTextComponent): Int {
        val borderInsets = textComponent.border.getBorderInsets(textComponent)
        return UIUtil.getLineHeight(textComponent) + borderInsets.top + borderInsets.bottom
    }

    override fun showProgress() {
        progressIcon.apply {
            isVisible = true
            resume()
        }
    }

    override fun hideProgress() {
        progressIcon.apply {
            isVisible = false
            suspend()
        }
    }

    override fun showTranslationPanel() {
        rightPanelLayout.show(rightPanel, CARD_TRANSLATION)
    }

    override fun showErrorPanel() {
        rightPanelLayout.show(rightPanel, CARD_ERROR)
        // 使用`SwingUtilities.invokeLater`似乎要比使用`Application.invokeLater`更好，
        // `Application.invokeLater`有时候会得不到想要的效果，对话框不会自动调整尺寸。
        SwingUtilities.invokeLater { mRoot.revalidate() }
    }

    override fun dispose() {
        Disposer.dispose(progressIcon)
    }

    private class TranslationTextArea(background: Color) : JBTextArea(1, 1) {

        init {
            isEditable = false
            this.background = background
        }

        override fun paintComponent(g: Graphics) {
            //region Fix #1025(https://github.com/YiiGuxing/TranslationPlugin/issues/1025)
            //强制绘制背景，以修复`Material Theme UI`主题所导致的显示异常。
            g.color = background
            g.fillRect(0, 0, width, height)
            //endregion

            super.paintComponent(g)
        }
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

    companion object {
        private const val CARD_TRANSLATION = "TRANSLATION"
        private const val CARD_ERROR = "ERROR"
    }
}