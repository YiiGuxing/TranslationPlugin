package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppStorage
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.CheckRegExpDialog
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
import java.awt.Font
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * SettingsPanel
 *
 * Created by Yii.Guxing on 2018/1/18
 */
class SettingsPanel(settings: Settings, appStorage: AppStorage)
    : SettingsForm(settings, appStorage), ConfigurablePanel {

    private var validRegExp = true

    override val component: JComponent = wholePanel

    init {
        primaryFontComboBox.fixFontComboBoxSize()
        phoneticFontComboBox.fixFontComboBoxSize()

        setTitles()
        setListeners()
        initSelectionModeComboBox()
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
                override fun customize(list: JList<*>, value: SelectionMode, index: Int, selected: Boolean,
                                       hasFocus: Boolean) {
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

    private fun setListeners() {
        fontCheckBox.addItemListener {
            val selected = fontCheckBox.isSelected
            primaryFontComboBox.isEnabled = selected
            phoneticFontComboBox.isEnabled = selected
            fontPreview.isEnabled = selected
            primaryFontLabel.isEnabled = selected
            phoneticFontLabel.isEnabled = selected
        }
        primaryFontComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                previewPrimaryFont(primaryFontComboBox.fontName)
            }
        }
        phoneticFontComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                previewPhoneticFont(phoneticFontComboBox.fontName)
            }
        }
        clearHistoriesButton.addActionListener { appStorage.clearHistories() }

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

    private fun previewPrimaryFont(primary: String?) {
        if (primary.isNullOrBlank()) {
            fontPreview.font = JBUI.Fonts.label(14f)
        } else {
            fontPreview.font = JBUI.Fonts.create(primary, 14)
        }
    }

    private fun previewPhoneticFont(primary: String?) {
        val document = fontPreview.styledDocument

        val font: Font = if (primary.isNullOrBlank()) {
            JBUI.Fonts.label(14f)
        } else {
            JBUI.Fonts.create(primary, 14)
        }

        val attributeSet = SimpleAttributeSet()
        StyleConstants.setFontFamily(attributeSet, font.family)
        document.setCharacterAttributes(4, 41, attributeSet, true)
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
                    || settings.ignoreRegExp != ignoreRegExp.text
                    || settings.isOverrideFont != fontCheckBox.isSelected
                    || settings.primaryFontFamily != primaryFontComboBox.fontName
                    || settings.phoneticFontFamily != phoneticFontComboBox.fontName
                    || settings.showStatusIcon != showStatusIconCheckBox.isSelected
                    || settings.foldOriginal != foldOriginalCheckBox.isSelected
                    || settings.keepFormat != keepFormatCheckBox.isSelected
                    || settings.showWordForms != showWordFormsCheckBox.isSelected
                    || settings.autoReplace != autoReplaceCheckBox.isSelected
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

        with(settings) {
            isOverrideFont = fontCheckBox.isSelected
            primaryFontFamily = primaryFontComboBox.fontName
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = selectionModeComboBox.currentMode
            showStatusIcon = showStatusIconCheckBox.isSelected
            foldOriginal = foldOriginalCheckBox.isSelected
            keepFormat = keepFormatCheckBox.isSelected
            showWordForms = showWordFormsCheckBox.isSelected
            autoReplace = autoReplaceCheckBox.isSelected

            if (validRegExp) {
                ignoreRegExp = this@SettingsPanel.ignoreRegExp.text
            }
        }
    }

    override fun reset() {
        transPanelContainer.reset()

        val settings = settings
        ignoreRegExp.text = settings.ignoreRegExp ?: ""
        fontCheckBox.isSelected = settings.isOverrideFont
        showStatusIconCheckBox.isSelected = settings.showStatusIcon
        foldOriginalCheckBox.isSelected = settings.foldOriginal
        keepFormatCheckBox.isSelected = settings.keepFormat
        showWordFormsCheckBox.isSelected = settings.showWordForms
        autoReplaceCheckBox.isSelected = settings.autoReplace
        primaryFontComboBox.fontName = settings.primaryFontFamily
        phoneticFontComboBox.fontName = settings.phoneticFontFamily
        previewPrimaryFont(settings.primaryFontFamily)
        previewPhoneticFont(settings.phoneticFontFamily)

        maxHistoriesSizeComboBox.editor.item = Integer.toString(appStorage.maxHistorySize)
        selectionModeComboBox.selected = settings.autoSelectionMode
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