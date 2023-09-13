package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.util.ByteSize
import cn.yiiguxing.plugin.translate.util.CacheService
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import cn.yiiguxing.plugin.translate.wordbook.WordBookState
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.EditorTextField
import com.intellij.ui.FontComboBox
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.runAsync
import java.awt.event.ItemEvent
import javax.swing.JComponent
import kotlin.math.max

/**
 * SettingsPanel
 */
class SettingsPanel(
    private val settings: Settings,
    private val states: TranslationStates
) : SettingsUi(), ConfigurableUi {

    private var validRegExp = true

    override val component: JComponent = wholePanel

    override val preferredFocusedComponent: JComponent = translationEngineComboBox

    init {
        primaryFontComboBox.fixFontComboBoxSize()
        phoneticFontComboBox.fixFontComboBoxSize()
        resetPrimaryLanguageComboBox(settings.translator)
        ignoreRegExp = createRegexEditorField()
        doLayout()
        setListeners()
        initWordbookStorageComponents()
        initCache()
        initSupport()
    }

    private fun initWordbookStorageComponents() {
        wordbookStoragePathField.addBrowseFolderListener(WordbookStoragePathBrowser(settings))
        resetWordbookStoragePathButton.addActionListener {
            WordbookStoragePathBrowser.restoreDefaultWordbookStorage(settings)
            wordbookStoragePathField.text = ""
        }

        fun updateEnabled(state: WordBookState) {
            val enabled = WordBookService.isStableState(state)
            wordbookStoragePathField.isEnabled = enabled
            resetWordbookStoragePathButton.isEnabled = enabled
        }

        updateEnabled(WordBookService.instance.state)
        WordBookService.instance.stateBinding.observe(this) { state, _ ->
            updateEnabled(state)
        }
    }

    private fun initCache() {
        var isClearing = false
        val labelRef = DisposableRef.create(this, cacheSizeLabel)
        clearCacheButton.addActionListener {
            if (isClearing) {
                return@addActionListener
            }
            isClearing = true

            runAsync {
                CacheService.evictAllDiskCaches()
                CacheService.getDiskCacheSize()
            }.finishOnUiThread(labelRef) { label, size ->
                isClearing = false
                label.text = ByteSize.format(size ?: 0L)
            }
        }

        runAsync { CacheService.getDiskCacheSize() }
            .successOnUiThread(labelRef) { label, size ->
                label.text = ByteSize.format(size)
            }
    }

    private fun initSupport() {
        supportLinkLabel.setListener({ _, _ -> SupportDialog.show() }, null)
    }

    private fun setListeners() {
        translationEngineComboBox.addItemListener { event ->
            if (event.stateChange != ItemEvent.SELECTED) {
                return@addItemListener
            }
            resetPrimaryLanguageComboBox(event.item as TranslationEngine)
        }
        primaryFontComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                primaryFontPreview.previewFont(primaryFontComboBox.fontName)
            }
        }
        phoneticFontComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                phoneticFontPreview.previewFont(phoneticFontComboBox.fontName)
            }
        }
        restoreDefaultButton.addActionListener {
            primaryFontComboBox.fontName = null
            phoneticFontComboBox.fontName = null
            primaryFontPreview.previewFont(null)
            phoneticFontPreview.previewFont(null)
        }
        clearHistoriesButton.addActionListener {
            states.clearHistories()
        }

        configureTranslationEngineLink.setListener({ _, _ ->
            translationEngineComboBox.selected?.showConfigurationDialog()
        }, null)

        val background = ignoreRegExp.background
        ignoreRegExp.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: DocumentEvent) {
                try {
                    e.document.text.takeUnless { it.isBlank() }?.toRegex()

                    if (!validRegExp) {
                        ignoreRegExp.background = background
                        ignoreRegExpMsg.text = null

                        validRegExp = true
                    }
                } catch (e: Exception) {
                    ignoreRegExp.background = BACKGROUND_COLOR_ERROR
                    ignoreRegExpMsg.text = e.message?.let { it.substring(0, it.indexOf('\n')) }
                    validRegExp = false
                }
            }
        })
    }

    private fun JComponent.previewFont(primary: String?) {
        font = if (primary.isNullOrBlank()) {
            UI.defaultFont
        } else {
            JBUI.Fonts.create(primary, UIUtil.getFontSize(UIUtil.FontSize.NORMAL).toInt())
        }
    }

    private fun createRegexEditorField(): EditorTextField = EditorTextField(
        "",
        ProjectManager.getInstance().defaultProject,
        FileTypeManager.getInstance().findFileTypeByName("RegExp") ?: FileTypes.PLAIN_TEXT
    )

    private fun resetPrimaryLanguageComboBox(engine: TranslationEngine) {
        val supportedTargetLanguages = engine.supportedTargetLanguages()
        primaryLanguageComboBox.model = CollectionComboBoxModel(supportedTargetLanguages)
        primaryLanguageComboBox.selected = settings.primaryLanguage
            ?.takeIf { it in supportedTargetLanguages }
            ?: engine.translator.defaultLangForLocale
    }

    override val isModified: Boolean
        get() {
            if (!validRegExp) {
                return false
            }

            val settings = settings
            return settings.translator != translationEngineComboBox.selected
                    || !settings.translator.isConfigured()
                    || settings.translator.primaryLanguage != primaryLanguageComboBox.selected
                    || settings.targetLanguageSelection != targetLangSelectionComboBox.selected
                    || settings.autoSelectionMode != SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
                    || settings.takeWordWhenDialogOpens != takeWordCheckBox.isSelected
                    || settings.separators != separatorTextField.text
                    || settings.ignoreRegex != ignoreRegExp.text
                    || settings.primaryFontFamily != primaryFontComboBox.fontName
                    || settings.primaryFontPreviewText != primaryFontPreview.text
                    || settings.phoneticFontFamily != phoneticFontComboBox.fontName
                    || settings.foldOriginal != foldOriginalCheckBox.isSelected
                    || settings.keepFormat != keepFormatCheckBox.isSelected
                    || settings.autoPlayTTS != autoPlayTTSCheckBox.isSelected
                    || settings.ttsSource != ttsSourceComboBox.selected
                    || settings.autoReplace != autoReplaceCheckBox.isSelected
                    || settings.selectTargetLanguageBeforeReplacement != selectTargetLanguageCheckBox.isSelected
                    || settings.showWordsOnStartup != showWordsOnStartupCheckBox.isSelected
                    || settings.showExplanation != showExplanationCheckBox.isSelected
                    || settings.translateDocumentation != translateDocumentationCheckBox.isSelected
                    || settings.showReplacementAction != showReplacementActionCheckBox.isSelected
                    || settings.showActionsInContextMenuOnlyWithSelection != showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected
                    || states.maxHistorySize != maxHistoriesSizeComboBox.item
        }


    override fun apply() {

        states.maxHistorySize = max(maxHistoriesSizeComboBox.item, 0)

        @Suppress("Duplicates")
        with(settings) {
            val selectedTranslator = translationEngineComboBox.selected ?: translator
            if (!selectedTranslator.isConfigured()) {
                throw ConfigurationException(
                    message(
                        "settings.translator.requires.configuration",
                        selectedTranslator.translatorName
                    )
                )
            }
            translator = selectedTranslator
            translator.primaryLanguage = primaryLanguageComboBox.selected ?: translator.primaryLanguage
            targetLanguageSelection = targetLangSelectionComboBox.selected ?: TargetLanguageSelection.DEFAULT
            primaryFontFamily = primaryFontComboBox.fontName
            primaryFontPreviewText = primaryFontPreview.text
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
            ttsSource = ttsSourceComboBox.selected ?: TTSSource.ORIGINAL
            separators = separatorTextField.text
            foldOriginal = foldOriginalCheckBox.isSelected
            keepFormat = keepFormatCheckBox.isSelected
            autoPlayTTS = autoPlayTTSCheckBox.isSelected
            autoReplace = autoReplaceCheckBox.isSelected
            selectTargetLanguageBeforeReplacement = selectTargetLanguageCheckBox.isSelected
            showWordsOnStartup = showWordsOnStartupCheckBox.isSelected
            showExplanation = showExplanationCheckBox.isSelected
            translateDocumentation = translateDocumentationCheckBox.isSelected
            showReplacementAction = showReplacementActionCheckBox.isSelected
            showActionsInContextMenuOnlyWithSelection = showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected
            takeWordWhenDialogOpens = takeWordCheckBox.isSelected

            if (validRegExp) {
                ignoreRegex = this@SettingsPanel.ignoreRegExp.text
            }
        }
    }

    @Suppress("Duplicates")
    override fun reset() {
        resetPrimaryLanguageComboBox(settings.translator)

        translationEngineComboBox.selected = settings.translator
        targetLangSelectionComboBox.selected = settings.targetLanguageSelection
        ignoreRegExp.text = settings.ignoreRegex
        separatorTextField.text = settings.separators
        foldOriginalCheckBox.isSelected = settings.foldOriginal
        keepFormatCheckBox.isSelected = settings.keepFormat
        autoPlayTTSCheckBox.isSelected = settings.autoPlayTTS
        autoReplaceCheckBox.isSelected = settings.autoReplace
        selectTargetLanguageCheckBox.isSelected = settings.selectTargetLanguageBeforeReplacement
        showWordsOnStartupCheckBox.isSelected = settings.showWordsOnStartup
        showExplanationCheckBox.isSelected = settings.showExplanation
        primaryFontComboBox.fontName = settings.primaryFontFamily
        phoneticFontComboBox.fontName = settings.phoneticFontFamily
        primaryFontPreview.previewFont(settings.primaryFontFamily)
        primaryFontPreview.text = settings.primaryFontPreviewText
        phoneticFontPreview.previewFont(settings.phoneticFontFamily)
        maxHistoriesSizeComboBox.item = states.maxHistorySize
        takeNearestWordCheckBox.isSelected = settings.autoSelectionMode == SelectionMode.EXCLUSIVE
        ttsSourceComboBox.selected = settings.ttsSource
        translateDocumentationCheckBox.isSelected = settings.translateDocumentation
        showReplacementActionCheckBox.isSelected = settings.showReplacementAction
        showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected =
            settings.showActionsInContextMenuOnlyWithSelection
        takeWordCheckBox.isSelected = settings.takeWordWhenDialogOpens
        wordbookStoragePathField.text = settings.wordbookStoragePath ?: ""
    }

    companion object {
        private val BACKGROUND_COLOR_ERROR = JBColor(0xffb1a0, 0x6e2b28)

        private fun FontComboBox.fixFontComboBoxSize() {
            val size = preferredSize
            size.width = size.height * 8
            preferredSize = size
        }
    }
}