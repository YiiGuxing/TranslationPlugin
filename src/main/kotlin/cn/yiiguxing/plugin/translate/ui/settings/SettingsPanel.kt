package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.ui.CheckRegExpDialog
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.util.SelectionMode
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import icons.Icons
import java.awt.Dimension
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

/**
 * SettingsPanel
 */
class SettingsPanel(settings: Settings, appStorage: AppStorage) : SettingsForm(settings, appStorage),
    ConfigurablePanel {

    private var validRegExp = true

    override val component: JComponent = wholePanel

    init {
        primaryFontComboBox.fixFontComboBoxSize()
        phoneticFontComboBox.fixFontComboBoxSize()

        setTitles()
        setListeners()
        initSeparatorsTextField()
        initSelectionModeComboBox()
        initTargetLangSelectionComboBox()
        initTTSSourceComboBox()
        initSupport()
    }

    @Suppress("InvalidBundleOrProperty")
    private fun setTitles() {
        translateSettingsPanel.setTitledBorder(message("settings.title.translate"))
        fontPanel.setTitledBorder(message("settings.title.font"))
        historyPanel.setTitledBorder(message("settings.title.history"))
        optionsPanel.setTitledBorder(message("settings.title.options"))
    }

    @Suppress("InvalidBundleOrProperty")
    private fun initSelectionModeComboBox() {
        with(selectionModeComboBox) {
            model = CollectionComboBoxModel(listOf(SelectionMode.INCLUSIVE, SelectionMode.EXCLUSIVE))
            renderer = object : ListCellRendererWrapper<SelectionMode>() {
                override fun customize(
                    list: JList<*>,
                    value: SelectionMode,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    when (value) {
                        SelectionMode.EXCLUSIVE -> {
                            setText("Exclusive")
                            setToolTipText(message("settings.tooltip.exclusive"))
                        }
                        else -> {
                            setText("Inclusive")
                            setToolTipText(message("settings.tooltip.inclusive"))
                        }
                    }
                }
            }
        }
    }

    private fun initTargetLangSelectionComboBox() {
        with(targetLangSelectionComboBox) {
            model = CollectionComboBoxModel(TargetLanguageSelection.values().asList())
            renderer = object : ListCellRendererWrapper<TargetLanguageSelection>() {
                override fun customize(
                    list: JList<*>,
                    value: TargetLanguageSelection,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    setText(value.displayName)
                }
            }
        }
    }

    private fun initTTSSourceComboBox() {
        with(ttsSourceComboBox) {
            model = CollectionComboBoxModel(TTSSource.values().asList())
            renderer = object : ListCellRendererWrapper<TTSSource>() {
                override fun customize(
                    list: JList<*>,
                    value: TTSSource,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    setText(value.displayName)
                }
            }
            preferredSize = Dimension(preferredSize.width, JBUI.scale(26))
        }
    }

    private fun initSeparatorsTextField() {
        separatorsTextField.document = object : PlainDocument() {
            override fun insertString(offset: Int, str: String?, attr: AttributeSet?) {
                val text = getText(0, length)
                val stringToInsert = str
                    ?.filter { it in ' '..'~' && !Character.isLetterOrDigit(it) && !text.contains(it) }
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

    private fun initSupport() {
        supportLinkLabel.icon = Icons.Support
        supportLinkLabel.setListener({ _, _ -> SupportDialog.show() }, null)
    }

    private fun setListeners() {
        fontCheckBox.addItemListener {
            val selected = fontCheckBox.isSelected
            primaryFontComboBox.isEnabled = selected
            phoneticFontComboBox.isEnabled = selected
            primaryFontPreview.isEnabled = selected
            primaryFontLabel.isEnabled = selected
            phoneticFontLabel.isEnabled = selected
            phoneticFontPreview.isEnabled = selected
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
        clearHistoriesButton.addActionListener {
            appStorage.clearHistories()
        }
        autoPlayTTSCheckBox.addItemListener {
            ttsSourceComboBox.isEnabled = autoPlayTTSCheckBox.isSelected
        }

        val ignoreRegExp = ignoreRegExp
        checkIgnoreRegExpButton.addActionListener {
            val project = ProjectManager.getInstance().defaultProject
            CheckRegExpDialog(project, ignoreRegExp.text) { newRegExp ->
                if (newRegExp != ignoreRegExp.text) {
                    ignoreRegExp.text = newRegExp
                }
            }.show()
        }

        val background = ignoreRegExp.background
        ignoreRegExp.addDocumentListener(object : DocumentAdapter() {
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

    override val isModified: Boolean
        get() {
            if (!validRegExp) {
                return false
            }

            val settings = settings
            return transPanelContainer.isModified
                    || appStorage.maxHistorySize != getMaxHistorySize()
                    || settings.autoSelectionMode != selectionModeComboBox.currentMode
                    || settings.targetLanguageSelection != targetLangSelectionComboBox.selected
                    || settings.separators != separatorsTextField.text
                    || settings.ignoreRegExp != ignoreRegExp.text
                    || settings.isOverrideFont != fontCheckBox.isSelected
                    || settings.primaryFontFamily != primaryFontComboBox.fontName
                    || settings.phoneticFontFamily != phoneticFontComboBox.fontName
                    || settings.showStatusIcon != showStatusIconCheckBox.isSelected
                    || settings.foldOriginal != foldOriginalCheckBox.isSelected
                    || settings.keepFormat != keepFormatCheckBox.isSelected
                    || settings.autoPlayTTS != autoPlayTTSCheckBox.isSelected
                    || settings.ttsSource != ttsSourceComboBox.selected
                    || settings.showWordForms != showWordFormsCheckBox.isSelected
                    || settings.autoReplace != autoReplaceCheckBox.isSelected
                    || settings.selectTargetLanguageBeforeReplacement != selectTargetLanguageCheckBox.isSelected
                    || settings.showWordsOnStartup != showWordsOnStartupCheckBox.isSelected
                    || settings.showExplanation != showExplanationCheckBox.isSelected
        }


    override fun apply() {
        transPanelContainer.apply()

        getMaxHistorySize().let {
            if (it >= 0) {
                appStorage.maxHistorySize = it
            }
        }

        if (settings.showWordForms != showWordFormsCheckBox.isSelected) {
            TranslateService.clearCaches()
        }

        @Suppress("Duplicates")
        with(settings) {
            isOverrideFont = fontCheckBox.isSelected
            primaryFontFamily = primaryFontComboBox.fontName
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = selectionModeComboBox.currentMode
            targetLanguageSelection = targetLangSelectionComboBox.selected ?: TargetLanguageSelection.DEFAULT
            ttsSource = ttsSourceComboBox.selected ?: TTSSource.ORIGINAL
            separators = separatorsTextField.text
            showStatusIcon = showStatusIconCheckBox.isSelected
            foldOriginal = foldOriginalCheckBox.isSelected
            keepFormat = keepFormatCheckBox.isSelected
            autoPlayTTS = autoPlayTTSCheckBox.isSelected
            showWordForms = showWordFormsCheckBox.isSelected
            autoReplace = autoReplaceCheckBox.isSelected
            selectTargetLanguageBeforeReplacement = selectTargetLanguageCheckBox.isSelected
            showWordsOnStartup = showWordsOnStartupCheckBox.isSelected
            showExplanation = showExplanationCheckBox.isSelected

            if (validRegExp) {
                ignoreRegExp = this@SettingsPanel.ignoreRegExp.text
            }
        }
    }

    @Suppress("Duplicates")
    override fun reset() {
        transPanelContainer.reset()

        val settings = settings
        ignoreRegExp.text = settings.ignoreRegExp ?: ""
        separatorsTextField.text = settings.separators
        fontCheckBox.isSelected = settings.isOverrideFont
        showStatusIconCheckBox.isSelected = settings.showStatusIcon
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
        phoneticFontPreview.previewFont(settings.phoneticFontFamily)

        maxHistoriesSizeComboBox.editor.item = appStorage.maxHistorySize.toString()
        selectionModeComboBox.selected = settings.autoSelectionMode
        targetLangSelectionComboBox.selected = settings.targetLanguageSelection
        ttsSourceComboBox.selected = settings.ttsSource
    }

    companion object {
        private val BACKGROUND_COLOR_ERROR = JBColor(0xffb1a0, 0x6e2b28)
        private val ComboBox<SelectionMode>.currentMode get() = selected ?: SelectionMode.INCLUSIVE

        private fun FontComboBox.fixFontComboBoxSize() {
            val size = preferredSize
            size.width = size.height * 8
            preferredSize = size
        }

        private fun JPanel.setTitledBorder(title: String) {
            border = IdeBorderFactory.createTitledBorder(title)
        }
    }
}