package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.CheckRegExpDialog
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.util.ByteSize
import cn.yiiguxing.plugin.translate.util.CacheService
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.EditorTextField
import com.intellij.ui.FontComboBox
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.intellij.lang.regexp.RegExpLanguage
import java.awt.event.ItemEvent
import java.lang.ref.WeakReference
import javax.swing.JComponent

/**
 * SettingsPanel
 */
class SettingsPanel(val settings: Settings, val appStorage: AppStorage) : SettingsForm(), ConfigurablePanel {

    private var validRegExp = true

    override val component: JComponent = wholePanel

    init {
        primaryFontComboBox.fixFontComboBoxSize()
        phoneticFontComboBox.fixFontComboBoxSize()
        primaryLanguageComboBox.model = CollectionComboBoxModel(Settings.instance.translator.supportedTargetLanguages())
        ignoreRegExp = createRegexEditorField()
        doLayout()
        setListeners()
        initCache()
        initSupport()
    }

    private fun initCache() {
        clearCacheButton.addActionListener {
            clearCacheButton.isEnabled = false
            val buttonRef = WeakReference(clearCacheButton)
            val labelRef = WeakReference(cacheSizeLabel)
            executeOnPooledThread {
                CacheService.evictAllDiskCaches()
                val size = CacheService.getDiskCacheSize()
                labelRef.get()?.text = ByteSize.format(size)
                buttonRef.get()?.isEnabled = true
            }
        }

        val labelRef = WeakReference(cacheSizeLabel)
        executeOnPooledThread {
            val size = CacheService.getDiskCacheSize()
            labelRef.get()?.text = ByteSize.format(size)
        }
    }

    private fun initSupport() {
        supportLinkLabel.setListener({ _, _ -> SupportDialog.show() }, null)
    }

    private fun setListeners() {
        translationEngineComboBox.addItemListener { e ->
            val engine = e.item as TranslationEngine
            primaryLanguageComboBox.model = CollectionComboBoxModel(engine.supportedTargetLanguages())
            val selectedLang = primaryLanguageComboBox.selected
            if (!engine.supportedTargetLanguages().contains(selectedLang)) {
                primaryLanguageComboBox.selected = engine.primaryLanguage
            }
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
            primaryFontComboBox.fontName = UI.defaultFont.fontName
            phoneticFontComboBox.fontName = UI.defaultFont.fontName
        }
        clearHistoriesButton.addActionListener {
            appStorage.clearHistories()
        }

        checkIgnoreRegExpButton.addActionListener {
            val project = ProjectManager.getInstance().defaultProject
            CheckRegExpDialog(project, ignoreRegExp.text) { newRegExp ->
                if (newRegExp != ignoreRegExp.text) {
                    ignoreRegExp.text = newRegExp
                }
            }.show()
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
        font = if (primary.isNullOrBlank()) UI.defaultFont else JBUI.Fonts.create(primary, 14)
    }

    private fun getMaxHistorySize(): Int {
        val size = maxHistoriesSizeComboBox.editor.item
        return (size as? String)?.toIntOrNull() ?: -1
    }

    private fun createRegexEditorField(): EditorTextField = EditorTextField(
        "",
        ProjectManager.getInstance().defaultProject,
        RegExpLanguage.INSTANCE.associatedFileType
    )

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
                    || settings.googleTranslateSettings.useTranslateGoogleCom != useTranslateGoogleComCheckBox.isSelected
                    || settings.autoSelectionMode != SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
                    || settings.takeWordWhenDialogOpens != takeWordCheckBox.isSelected
                    || settings.separators != separatorsTextField.text
                    || settings.ignoreRegex != ignoreRegExp.text
                    || settings.primaryFontFamily != primaryFontComboBox.fontName
                    || settings.primaryFontPreviewText != primaryFontPreview.text
                    || settings.phoneticFontFamily != phoneticFontComboBox.fontName
                    || settings.foldOriginal != foldOriginalCheckBox.isSelected
                    || settings.keepFormat != keepFormatCheckBox.isSelected
                    || settings.autoPlayTTS != autoPlayTTSCheckBox.isSelected
                    || settings.ttsSource != ttsSourceComboBox.selected
                    || settings.showWordForms != showWordFormsCheckBox.isSelected
                    || settings.autoReplace != autoReplaceCheckBox.isSelected
                    || settings.selectTargetLanguageBeforeReplacement != selectTargetLanguageCheckBox.isSelected
                    || settings.showWordsOnStartup != showWordsOnStartupCheckBox.isSelected
                    || settings.showExplanation != showExplanationCheckBox.isSelected
                    || settings.translateDocumentation != translateDocumentationCheckBox.isSelected
                    || settings.showActionsInContextMenuOnlyWithSelection != showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected
                    || appStorage.maxHistorySize != getMaxHistorySize()
        }


    override fun apply() {

        getMaxHistorySize().let {
            if (it >= 0) {
                appStorage.maxHistorySize = it
            }
        }

        @Suppress("Duplicates")
        with(settings) {
            val selectedTranslator = translationEngineComboBox.selected ?: translator
            if (!selectedTranslator.isConfigured()) {
                throw ConfigurationException(message("settings.translator.requires.configuration", selectedTranslator.translatorName))
            }
            translator = selectedTranslator
            translator.primaryLanguage = primaryLanguageComboBox.selected ?: translator.primaryLanguage
            targetLanguageSelection = targetLangSelectionComboBox.selected ?: TargetLanguageSelection.DEFAULT
            googleTranslateSettings.useTranslateGoogleCom = useTranslateGoogleComCheckBox.isSelected
            primaryFontFamily = primaryFontComboBox.fontName
            primaryFontPreviewText = primaryFontPreview.text
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
            ttsSource = ttsSourceComboBox.selected ?: TTSSource.ORIGINAL
            separators = separatorsTextField.text
            foldOriginal = foldOriginalCheckBox.isSelected
            keepFormat = keepFormatCheckBox.isSelected
            autoPlayTTS = autoPlayTTSCheckBox.isSelected
            showWordForms = showWordFormsCheckBox.isSelected
            autoReplace = autoReplaceCheckBox.isSelected
            selectTargetLanguageBeforeReplacement = selectTargetLanguageCheckBox.isSelected
            showWordsOnStartup = showWordsOnStartupCheckBox.isSelected
            showExplanation = showExplanationCheckBox.isSelected
            translateDocumentation = translateDocumentationCheckBox.isSelected
            showActionsInContextMenuOnlyWithSelection = showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected
            takeWordWhenDialogOpens = takeWordCheckBox.isSelected

            if (validRegExp) {
                ignoreRegex = this@SettingsPanel.ignoreRegExp.text
            }
        }
    }

    @Suppress("Duplicates")
    override fun reset() {
        translationEngineComboBox.selected = settings.translator
        primaryLanguageComboBox.selected = settings.translator.primaryLanguage
        targetLangSelectionComboBox.selected = settings.targetLanguageSelection
        useTranslateGoogleComCheckBox.isSelected = settings.googleTranslateSettings.useTranslateGoogleCom
        ignoreRegExp.text = settings.ignoreRegex
        separatorsTextField.text = settings.separators
        foldOriginalCheckBox.isSelected = settings.foldOriginal
        keepFormatCheckBox.isSelected = settings.keepFormat
        autoPlayTTSCheckBox.isSelected = settings.autoPlayTTS
        showWordFormsCheckBox.isSelected = settings.showWordForms
        autoReplaceCheckBox.isSelected = settings.autoReplace
        selectTargetLanguageCheckBox.isSelected = settings.selectTargetLanguageBeforeReplacement
        showWordsOnStartupCheckBox.isSelected = settings.showWordsOnStartup
        showExplanationCheckBox.isSelected = settings.showExplanation
        primaryFontComboBox.fontName = settings.primaryFontFamily
        phoneticFontComboBox.fontName = settings.phoneticFontFamily
        primaryFontPreview.previewFont(settings.primaryFontFamily)
        primaryFontPreview.text = settings.primaryFontPreviewText
        phoneticFontPreview.previewFont(settings.phoneticFontFamily)
        maxHistoriesSizeComboBox.editor.item = appStorage.maxHistorySize.toString()
        takeNearestWordCheckBox.isSelected = settings.autoSelectionMode == SelectionMode.EXCLUSIVE
        ttsSourceComboBox.selected = settings.ttsSource
        translateDocumentationCheckBox.isSelected = settings.translateDocumentation
        showActionsInContextMenuOnlyWithSelectionCheckbox.isSelected = settings.showActionsInContextMenuOnlyWithSelection
        takeWordCheckBox.isSelected = settings.takeWordWhenDialogOpens
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