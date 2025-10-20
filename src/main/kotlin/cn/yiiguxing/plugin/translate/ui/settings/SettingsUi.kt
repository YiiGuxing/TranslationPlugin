package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.TTSSource
import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.TTSEngine
import cn.yiiguxing.plugin.translate.ui.TypedComboBoxEditor
import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.UI.fill
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.fillY
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.migLayoutVertical
import cn.yiiguxing.plugin.translate.ui.UI.migSize
import cn.yiiguxing.plugin.translate.ui.UI.plus
import cn.yiiguxing.plugin.translate.ui.UI.wrap
import cn.yiiguxing.plugin.translate.ui.WindowLocation
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.util.IdeVersion
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.TranslationIcons
import net.miginfocom.layout.CC
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.util.function.Supplier
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

internal abstract class SettingsUi {
    protected val wholePanel: JPanel = JPanel()

    protected val translationEngineComboBox: ComboBox<TranslationEngine> = comboBox<TranslationEngine>().apply {
        isSwingPopup = false
        renderer = object : GroupedComboBoxRenderer<TranslationEngine>() {
            override fun getIcon(item: TranslationEngine): Icon = item.icon
            override fun getText(item: TranslationEngine): String = item.translatorName
            override fun separatorFor(value: TranslationEngine): ListSeparator? = null
        }

        addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                fixEngineConfigurationComponent()
            }
        }
    }

    private lateinit var configureTranslationEngineHandler: () -> Unit

    private val configureTranslationEngineButton: ActionButton =
        configureButton({ message("settings.action.configure.translation.engine") }) {
            configureTranslationEngineHandler()
        }

    protected fun onConfigureTranslationEngine(handler: () -> Unit) {
        configureTranslationEngineHandler = handler
    }

    protected val ttsEngineComboBox: ComboBox<TTSEngine> = comboBox<TTSEngine>().apply {
        isSwingPopup = false
        renderer = object : GroupedComboBoxRenderer<TTSEngine>() {
            override fun getIcon(item: TTSEngine): Icon = item.icon
            override fun getText(item: TTSEngine): String = item.ttsName
            override fun separatorFor(value: TTSEngine): ListSeparator? = null
        }
        addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                fixTtsEngineConfigurationComponent()
            }
        }
    }

    private lateinit var configureTTSEngineHandler: () -> Unit

    private val configureTtsEngineButton: ActionButton =
        configureButton({ message("settings.action.configure.tts.engine") }) {
            configureTTSEngineHandler()
        }

    protected fun onConfigureTTSEngine(handler: () -> Unit) {
        configureTTSEngineHandler = handler
    }

    protected val primaryLanguageComboBox: ComboBox<Lang> = comboBox<Lang>().apply {
        isSwingPopup = false
        renderer = object : GroupedComboBoxRenderer<Lang>() {
            override fun getText(item: Lang): String = item.localeName
            override fun separatorFor(value: Lang): ListSeparator? = null
        }
    }

    protected val sourceLanguageComboBox = LanguageSelectionComboBox()
    protected val targetLanguageComboBox = LanguageSelectionComboBox()

    protected val takeWordCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.take.word.when.translation.dialog.opens"))
    protected val takeNearestWordCheckBox: JCheckBox = JCheckBox(message("settings.options.take.single.word"))
    protected val keepFormatCheckBox: JBCheckBox = JBCheckBox(message("settings.options.keepFormatting"))

    protected lateinit var ignoreRegExp: EditorTextField
    protected val ignoreRegExpMsg: JLabel = JLabel().apply { foreground = ERROR_FOREGROUND_COLOR }

    protected val separatorTextField: JTextField = JTextField().apply {
        document = object : PlainDocument() {
            override fun insertString(offset: Int, str: String?, attr: AttributeSet?) {
                val text = getText(0, length)
                val stringToInsert = str
                    ?.filter { (it in ' '..'~' || it == '\t') && !Character.isLetterOrDigit(it) && !text.contains(it) }
                    ?.toSet()
                    ?.take(10 - length)
                    ?.joinToString("")
                    ?: return
                if (stringToInsert.isNotEmpty()) {
                    super.insertString(offset, stringToInsert, attr)
                }
            }
        }
    }

    protected var primaryFontComboBox: FontComboBox = createFontComboBox(filterNonLatin = false)
    protected var phoneticFontComboBox: FontComboBox = createFontComboBox(filterNonLatin = true)

    protected val primaryFontPreview: JTextComponent = JEditorPane(
        "text/plain",
        message("settings.font.default.preview.text")
    )

    protected val phoneticFontPreview: JLabel = ComponentPanelBuilder.createCommentComponent(
        PHONETIC_CHARACTERS, true, 50, true
    )
    protected val restoreDefaultButton = JButton(message("settings.button.restore.default"))

    protected val foldOriginalCheckBox: JBCheckBox = JBCheckBox(message("settings.options.foldOriginal"))

    protected val ttsSourceComboBox: ComboBox<TTSSource> =
        ComboBox(CollectionComboBoxModel(TTSSource.entries)).apply {
            renderer = SimpleListCellRenderer.create("") { it.displayName }
        }

    protected val autoPlayTTSCheckBox: JBCheckBox = JBCheckBox(message("settings.options.autoPlayTTS")).apply {
        addItemListener {
            ttsSourceComboBox.isEnabled = isSelected
        }
    }

    protected val autoReplaceCheckBox: JBCheckBox = JBCheckBox(message("settings.options.autoReplace"))
    protected val showReplacementActionCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.show.replacement.action"))
    protected val replacementTranslateLanguageSelectionCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.pre-translation.language.selection"))
    protected val useLastReplacementTranslateLanguageCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.use.last.replacement.translate.languages"))
    protected val showWordsOnStartupCheckBox: JBCheckBox = JBCheckBox(message("settings.options.showWordsOnStartup"))
    protected val wordbookStoragePathField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        isEditable = false
        textField.isFocusable = false
        (textField as? JBTextField)?.emptyText?.text = TranslationStorages.DATA_DIRECTORY.toString()
    }
    protected val resetWordbookStoragePathButton: JButton = JButton(message("settings.button.restore.default"))
    protected val showExplanationCheckBox: JBCheckBox = JBCheckBox(message("settings.options.showExplanation"))

    protected val maxHistoriesSizeComboBox: ComboBox<Int> = comboBox(50, 30, 20, 10).apply {
        editor = TypedComboBoxEditor { (it.toDoubleOrNull()?.toInt() ?: 50).coerceIn(10, 5000) }
        isEditable = true
    }

    protected val clearHistoriesButton: JButton = JButton(message("settings.clear.history.button"))

    protected val cacheSizeLabel: JLabel = JLabel("0KB")
    protected val clearCacheButton: JButton = JButton(message("settings.cache.button.clear"))

    protected val translateDocumentationCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.translate.documentation"))

    protected val showActionsInContextMenuOnlyWithSelectionCheckbox: JBCheckBox =
        JBCheckBox(message("settings.options.show.actions.only.with.selection"))

    protected val translationWindowLocationComboBox: ComboBox<WindowLocation> = comboBox<WindowLocation>().apply {
        renderer = SimpleListCellRenderer.create("") { it.displayName }
    }

    protected val supportLinkLabel: LinkLabel<*> =
        LinkLabel<Any>(message("support.or.donate"), TranslationIcons.Support).apply {
            border = JBUI.Borders.emptyTop(20)
        }

    protected fun doLayout() {
        val generalPanel = titledPanel(message("settings.panel.title.general")) {
            val comboboxGroup = "combobox"

            add(JLabel(message("settings.label.translation.engine")))
            add(translationEngineComboBox, CC().sizeGroupX(comboboxGroup).minWidth(migSize(200)))
            val configurePanel = Box.createHorizontalBox().apply {
                add(configureTranslationEngineButton)
                fixEngineConfigurationComponent()
            }
            add(configurePanel, wrap().span(2))

            add(JLabel(message("settings.label.tts.engine")))
            add(ttsEngineComboBox, CC().sizeGroupX(comboboxGroup))
            val ttsConfigurePanel = Box.createHorizontalBox().apply {
                add(configureTtsEngineButton)
                fixTtsEngineConfigurationComponent()
            }
            add(ttsConfigurePanel, wrap().span(2))

            add(JLabel(message("settings.label.primaryLanguage")))
            add(primaryLanguageComboBox, wrap().sizeGroupX(comboboxGroup))

            add(JLabel(message("settings.label.source.language")))
            add(sourceLanguageComboBox, wrap().sizeGroupX(comboboxGroup))

            add(JLabel(message("settings.label.targetLanguage")))
            add(targetLanguageComboBox, wrap().sizeGroupX(comboboxGroup))
        }
        val textSelectionPanel = titledPanel(message("settings.panel.title.text.selection"), true) {
            add(keepFormatCheckBox, wrap().span(4))
            add(takeNearestWordCheckBox, wrap().span(4))
            add(takeWordCheckBox, wrap().span(4))

            add(JLabel(message("settings.label.ignore")))

            val ignoreRegexComponent: JComponent =
                if (ApplicationManager.getApplication() != null) ignoreRegExp
                else JTextField()

            add(ignoreRegexComponent, fillX())

            val msgCC = fillX()
                .gapBefore(JBUIScale.scale(2).toString())
                .gapTop(JBUIScale.scale(2).toString())
                .cell(1, 4)
                .wrap()
            add(ignoreRegExpMsg, msgCC)

            val comment = ComponentPanelBuilder.createCommentComponent(message("settings.comment.ignore"), true)
            val commentCC = fillX()
                .gapBefore(JBUIScale.scale(2).toString())
                .gapTop(JBUIScale.scale(2).toString())
                .cell(1, 5)
                .wrap()
            add(comment, commentCC)
        }

        val fontsPanel = titledPanel(message("settings.panel.title.fonts")) {
            add(JLabel(message("settings.font.label.primary")))
            add(JLabel(message("settings.font.label.phonetic")), wrap())
            add(primaryFontComboBox)
            add(phoneticFontComboBox, wrap())

            val primaryPreviewPanel = JPanel(migLayout()).apply {
                add(primaryFontPreview, fill())
            }
            val phoneticPreviewPanel = JPanel(migLayout()).apply {
                add(phoneticFontPreview, fill())
            }
            add(primaryPreviewPanel, fill())
            add(phoneticPreviewPanel, fill().wrap())

            add(restoreDefaultButton)

            //compensate custom border of ComboBox
            primaryFontPreview.border = emptyBorder(3) + JBUI.Borders.customLine(JBColor.border())
            primaryPreviewPanel.border = emptyBorder(3)
            phoneticFontPreview.border = emptyBorder(6)
        }

        val translationPopupPanel = titledPanel(message("settings.panel.title.translation.popup")) {
            add(foldOriginalCheckBox, wrap())
            add(hBox(autoPlayTTSCheckBox, ttsSourceComboBox), wrap())
            add(
                hBox(
                    JLabel(message("settings.label.translation.dialog.location")),
                    translationWindowLocationComboBox,
                    ContextHelpLabel.create(message("window.location.help.tip"))
                ),
                wrap()
            )
        }

        val translateAndReplacePanel = titledPanel(message("settings.panel.title.replace.with.translation")) {
            add(replacementTranslateLanguageSelectionCheckBox, wrap().span(3))
            add(useLastReplacementTranslateLanguageCheckBox, wrap().span(3).gapBefore(migSize(20)))
            add(showReplacementActionCheckBox, wrap().span(3))
            add(autoReplaceCheckBox, wrap().span(3))
            add(JLabel(message("settings.label.separators")))
            add(separatorTextField)
            add(ContextHelpLabel.create(message("settings.tip.separators")), wrap())

            setMinWidth(separatorTextField, JBUIScale.scale(250))
        }

        val wordOfTheDayPanel = titledPanel(message("settings.panel.title.word.of.the.day")) {
            add(showWordsOnStartupCheckBox, wrap())
            add(showExplanationCheckBox, wrap())
        }

        val wordbookStorePanel = titledPanel(message("settings.panel.title.word.book"), true) {
            add(JBLabel(message("settings.wordbook.label.storage.path")), CC().gapAfter(JBUIScale.scale(4).toString()))
            add(wordbookStoragePathField, fillX())
            add(resetWordbookStoragePathButton, wrap())

            val comment = ComponentPanelBuilder.createCommentComponent(
                message("settings.wordbook.label.storage.path.tips"), true
            )
            val commentCC = fillX()
                .gapBefore(JBUIScale.scale(2).toString())
                .gapTop(JBUIScale.scale(2).toString())
                .spanX(2)
                .cell(1, 1)
            add(comment, commentCC)
        }

        val cacheAndHistoryPanel = titledPanel(message("settings.panel.title.cacheAndHistory")) {
            add(JPanel(migLayout()).apply {
                add(JLabel(message("settings.cache.label.diskCache")))
                add(cacheSizeLabel, wrap().gapBefore(JBUIScale.scale(2).toString()))
            })
            add(clearCacheButton, wrap().span(2))

            add(JLabel(message("settings.history.label.maxLength")))
            add(maxHistoriesSizeComboBox)
            add(clearHistoriesButton, wrap())
            setMinWidth(maxHistoriesSizeComboBox)
        }

        val otherPanel = titledPanel(message("settings.panel.title.other")) {
            if (isSupportDocumentTranslation()) {
                add(translateDocumentationCheckBox, wrap())
            }
            add(showActionsInContextMenuOnlyWithSelectionCheckbox, wrap())
        }

        wholePanel.addVertically(
            generalPanel,
            fontsPanel,
            textSelectionPanel,
            translationPopupPanel,
            translateAndReplacePanel,
            wordOfTheDayPanel,
            wordbookStorePanel,
            cacheAndHistoryPanel,
            otherPanel,
            supportLinkLabel
        )
    }

    fun createMainPanel(): JPanel {
        doLayout()
        return wholePanel
    }

    private fun fixEngineConfigurationComponent() {
        configureTranslationEngineButton.isVisible = translationEngineComboBox.selected?.hasConfiguration ?: false
    }

    private fun fixTtsEngineConfigurationComponent() {
        configureTtsEngineButton.isVisible = ttsEngineComboBox.selected?.configurable ?: false
    }

    open fun isSupportDocumentTranslation(): Boolean {
        // Documentation translation is not supported before Rider 2022.1.
        return IdeVersion >= IdeVersion.IDE2022_1 || IdeVersion.buildNumber.productCode != "RD"
    }

    companion object {
        private const val PHONETIC_CHARACTERS =
            "iː əː ɔː uː aː i ə ɔ u æ e ʌ aɪ eɪ ɔɪ ɪə ɛə uə əu au p t k f s θ ʃ tʃ tr ts b d g v z ð ʒ dʒ dr dz m n ŋ h r l w j"

        private const val MIN_ELEMENT_WIDTH = 80

        private val ERROR_FOREGROUND_COLOR = UIUtil.getErrorForeground()

        private fun setMinWidth(component: JComponent, minWidth: Int = JBUIScale.scale(MIN_ELEMENT_WIDTH)) =
            component.apply {
                minimumSize = Dimension(minWidth, height)
            }

        private fun createFontComboBox(filterNonLatin: Boolean): FontComboBox =
            FontComboBox(false, filterNonLatin, false)

        private fun titledPanel(title: String, fill: Boolean = false, body: JPanel.() -> Unit): JComponent {
            val innerPanel = JPanel(migLayout(migSize(4)))
            innerPanel.body()
            return JPanel(migLayout()).apply {
                border = IdeBorderFactory.createTitledBorder(title)
                if (fill) {
                    add(innerPanel, fillX())
                } else {
                    add(innerPanel)
                    add(JPanel(), fillX())
                }
            }
        }

        private fun JPanel.addVertically(vararg components: JComponent) {
            layout = migLayoutVertical()
            components.forEach {
                add(it, fillX())
            }
            add(JPanel(), fillY())
        }

        private fun hBox(vararg components: JComponent): JPanel = JPanel().apply {
            layout = HorizontalLayout(JBUIScale.scale(4))
            components.forEach { add(it) }
        }

        private fun <T> comboBox(elements: List<T>): ComboBox<T> = ComboBox(CollectionComboBoxModel(elements))

        @Suppress("SameParameterValue")
        private fun <T> comboBox(vararg elements: T): ComboBox<T> = comboBox(elements.toList())

        private inline fun <reified T : Enum<T>> comboBox(): ComboBox<T> = comboBox(enumValues<T>().toList())

        private fun configureButton(actionText: Supplier<String>, action: () -> Unit): ActionButton {
            return ActionButton(
                object : AnAction(actionText, actionText, AllIcons.General.Settings) {
                    override fun actionPerformed(e: AnActionEvent) = action()
                },
                null,
                ActionPlaces.UNKNOWN,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            )
        }
    }
}