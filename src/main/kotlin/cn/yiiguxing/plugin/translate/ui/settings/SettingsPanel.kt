package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.CheckRegExpDialog
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.form.AppKeySettingsDialog
import cn.yiiguxing.plugin.translate.ui.form.AppKeySettingsPanel
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.util.ByteSize
import cn.yiiguxing.plugin.translate.util.CacheService
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
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

    private val youdaoAppKeySettingsPanel = AppKeySettingsPanel(
        IconLoader.getIcon("/image/youdao_translate_logo.png"),
        YOUDAO_AI_URL,
        settings.youdaoTranslateSettings
    )

    private val baiduAppKeySettingsPanel = AppKeySettingsPanel(
        IconLoader.getIcon("/image/baidu_translate_logo.png"),
        BAIDU_FANYI_URL,
        settings.baiduTranslateSettings
    )

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
            when (translationEngineComboBox.selected) {
                TranslationEngine.YOUDAO -> AppKeySettingsDialog(
                    message("settings.youdao.title"),
                    youdaoAppKeySettingsPanel
                ).show()
                TranslationEngine.BAIDU -> AppKeySettingsDialog(
                    message("settings.baidu.title"),
                    baiduAppKeySettingsPanel
                ).show()
                else -> throw RuntimeException("Configure link should be available only for Youdao and Baidu engines, was: ${translationEngineComboBox.selected}")
            }
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
        RegExpLanguage.INSTANCE.getAssociatedFileType()
    )

    override val isModified: Boolean
        get() {
            if (!validRegExp) {
                return false
            }

            val settings = settings
            return settings.translator != translationEngineComboBox.selected
                    || settings.translator.primaryLanguage != primaryLanguageComboBox.selected
                    || appStorage.maxHistorySize != getMaxHistorySize()
                    || settings.autoSelectionMode != SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
                    || settings.targetLanguageSelection != targetLangSelectionComboBox.selected
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
        }


    override fun apply() {

        getMaxHistorySize().let {
            if (it >= 0) {
                appStorage.maxHistorySize = it
            }
        }

        @Suppress("Duplicates")
        with(settings) {
            translator = translationEngineComboBox.selected ?: translator
            translator.primaryLanguage = primaryLanguageComboBox.selected ?: translator.primaryLanguage
            primaryFontFamily = primaryFontComboBox.fontName
            primaryFontPreviewText = primaryFontPreview.text
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = SelectionMode.takeNearestWord(takeNearestWordCheckBox.isSelected)
            targetLanguageSelection = targetLangSelectionComboBox.selected ?: TargetLanguageSelection.DEFAULT
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

            if (validRegExp) {
                ignoreRegex = this@SettingsPanel.ignoreRegExp.text
            }
        }
    }

    @Suppress("Duplicates")
    override fun reset() {
        translationEngineComboBox.selected = settings.translator
        primaryLanguageComboBox.selected = settings.translator.primaryLanguage
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
        targetLangSelectionComboBox.selected = settings.targetLanguageSelection
        ttsSourceComboBox.selected = settings.ttsSource
        translateDocumentationCheckBox.isSelected = settings.translateDocumentation
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