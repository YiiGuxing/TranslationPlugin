package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppStorage
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import cn.yiiguxing.plugin.translate.util.SelectionMode
import com.intellij.ui.FontComboBox
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ListCellRendererWrapper
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

    override val component: JComponent = wholePanel

    init {
        primaryFontComboBox.fixFontComboBoxSize()
        phoneticFontComboBox.fixFontComboBoxSize()

        setTitles()
        setRenderer()
        setListeners()
    }

    @Suppress("InvalidBundleOrProperty")
    private fun setTitles() {
        selectionSettingsPanel.setTitledBorder(message("settings.title.selectionMode"))
        fontPanel.setTitledBorder(message("settings.title.font"))
        historyPanel.setTitledBorder(message("settings.title.history"))
    }

    @Suppress("InvalidBundleOrProperty")
    private fun setRenderer() {
        selectionModeComboBox.renderer = object : ListCellRendererWrapper<String>() {
            override fun customize(list: JList<*>, value: String, index: Int, selected: Boolean, hasFocus: Boolean) {
                setText(value)
                if (index == INDEX_INCLUSIVE) {
                    setToolTipText(message("settings.tooltip.inclusive"))
                } else if (index == INDEX_EXCLUSIVE) {
                    setToolTipText(message("settings.tooltip.exclusive"))
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

    private fun getAutoSelectionMode(): SelectionMode {
        return if (selectionModeComboBox.selectedIndex == INDEX_INCLUSIVE) {
            SelectionMode.INCLUSIVE
        } else {
            SelectionMode.EXCLUSIVE
        }
    }

    private fun getMaxHistorySize(): Int {
        val size = maxHistoriesSizeComboBox.editor.item
        if (size is String) {
            try {
                return Integer.parseInt(size)
            } catch (e: NumberFormatException) {
                /*no-op*/
            }
        }

        return -1
    }


    override val isModified: Boolean
        get() = (transPanelContainer.isModified ||
                settings.autoSelectionMode !== getAutoSelectionMode() ||
                appStorage.maxHistorySize != getMaxHistorySize() ||
                settings.isOverrideFont != fontCheckBox.isSelected ||
                settings.primaryFontFamily != primaryFontComboBox.fontName ||
                settings.phoneticFontFamily != phoneticFontComboBox.fontName)


    override fun apply() {
        transPanelContainer.apply()

        getMaxHistorySize().let {
            if (it >= 0) {
                appStorage.maxHistorySize = it
            }
        }

        with(settings) {
            isOverrideFont = fontCheckBox.isSelected
            primaryFontFamily = primaryFontComboBox.fontName
            phoneticFontFamily = phoneticFontComboBox.fontName
            autoSelectionMode = getAutoSelectionMode()
        }
    }

    override fun reset() {
        transPanelContainer.reset()

        val settings = settings
        fontCheckBox.isSelected = settings.isOverrideFont
        primaryFontComboBox.fontName = settings.primaryFontFamily
        phoneticFontComboBox.fontName = settings.phoneticFontFamily
        previewPrimaryFont(settings.primaryFontFamily)
        previewPhoneticFont(settings.phoneticFontFamily)

        maxHistoriesSizeComboBox.editor.item = Integer.toString(appStorage.maxHistorySize)
        selectionModeComboBox.selectedIndex = if (settings.autoSelectionMode === SelectionMode.INCLUSIVE) {
            INDEX_INCLUSIVE
        } else {
            INDEX_EXCLUSIVE
        }
    }

    companion object {
        private const val INDEX_INCLUSIVE = 0
        private const val INDEX_EXCLUSIVE = 1

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