package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppStorage
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.ui.FixedSizeCardLayout
import cn.yiiguxing.plugin.translate.ui.form.TranslatorSettingsContainerForm
import cn.yiiguxing.plugin.translate.ui.selected
import com.intellij.ui.ListCellRendererWrapper
import java.awt.event.ItemEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JList

/**
 * TranslatorSettingsContainer
 *
 * Created by Yii.Guxing on 2018/1/18
 */
class TranslatorSettingsContainer(
        private val settings: Settings,
        private val appStorage: AppStorage
) : TranslatorSettingsContainerForm<TranslatorSettingsPanel>(), ConfigurablePanel {

    private val layout = FixedSizeCardLayout()

    override val component: JComponent = this

    init {
        contentPanel.layout = layout

        add(GoogleTranslateSettingsPanel(settings.googleTranslateSettings))
        add(YoudaoTranslateSettingsPanel(settings.youdaoTranslateSettings))

        comboBox.renderer = object : ListCellRendererWrapper<TranslatorSettingsPanel>() {
            override fun customize(list: JList<*>, value: TranslatorSettingsPanel, index: Int, selected: Boolean,
                                   hasFocus: Boolean) {
                setText(value.name)
                setIcon(value.icon)
            }
        }
        comboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                layout.show(contentPanel, (it.item as TranslatorSettingsPanel).id)
            }
        }
    }

    private fun add(panel: TranslatorSettingsPanel) {
        contentPanel.add(panel.id, panel.component)
        comboBox.addItem(panel)
    }


    override val isModified: Boolean
        get() = with(comboBox) {
            selected?.id != settings.translator ||
                    (0 until itemCount).any { getItemAt(it).isModified }
        }

    override fun reset() {
        val translator = settings.translator
        for (i in 0 until comboBox.itemCount) {
            val selectedPanel = comboBox.getItemAt(i)
            if (selectedPanel.id == translator) {
                comboBox.selectedIndex = i
            }
            selectedPanel.reset()
        }
    }

    override fun apply() {
        val selectedPanel = comboBox.selected
        if (selectedPanel != null) {
            val oldTranslator = settings.translator
            val newTranslator = selectedPanel.id
            if (oldTranslator != newTranslator) {
                appStorage.lastSourceLanguage = null
                appStorage.lastTargetLanguage = null
            }

            settings.translator = newTranslator
        }

        with(comboBox) {
            for (i in 0 until itemCount) {
                getItemAt(i).apply()
            }
        }
    }
}

interface TranslatorSettingsPanel : ConfigurablePanel {
    val id: String
    val name: String
    val icon: Icon
}