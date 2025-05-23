package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.LanguageSelection
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SeparatorWithText
import java.awt.Component
import javax.swing.JList


internal class LanguageSelectionComboBox : ComboBox<Any>() {

    val languageSelection: LanguageSelection?
        get() = when (selectedItem) {
            is Lang -> null
            is LanguageSelection -> selectedItem as LanguageSelection
            else -> LanguageSelection.MAIN_OR_ENGLISH
        }

    val language: Lang?
        get() = when (selectedItem) {
            is Lang -> selectedItem as Lang
            else -> null
        }

    init {
        isSwingPopup = false
        renderer = Renderer()
    }

    fun setLanguages(languages: List<Lang>) {
        val languageSelections = LanguageSelection.values()
        val items = ArrayList<Any>(languageSelections.size + languages.size + 1)
        items.addAll(LanguageSelection.values())
        items.add(LanguageSelectionSeparator)
        items.addAll(languages)

        model = object : CollectionComboBoxModel<Any>(items, fixSelection(items, selectedItem)) {
            override fun setSelectedItem(item: Any?) {
                if (item is Lang || item is LanguageSelection) {
                    super.setSelectedItem(fixSelection(internalList, item))
                }
            }
        }
    }

    private object LanguageSelectionSeparator

    private companion object {
        fun fixSelection(items: List<Any?>, item: Any?): Any {
            return item?.takeIf { it !== LanguageSelectionSeparator && it in items }
                ?: Lang.AUTO.takeIf { it in items }
                ?: LanguageSelection.MAIN_OR_ENGLISH
        }
    }

    private class Renderer : ColoredListCellRenderer<Any>() {
        private val separator = SeparatorWithText().apply {
            caption = message("language.selection.combo.box.languages")
        }

        override fun getListCellRendererComponent(
            list: JList<out Any?>?,
            value: Any?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ): Component {
            if (value === LanguageSelectionSeparator) {
                return separator
            }
            return super.getListCellRendererComponent(list, value, index, selected, hasFocus)
        }

        override fun customizeCellRenderer(
            list: JList<out Any?>,
            value: Any?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            val text = when (value) {
                is Lang -> value.localeName
                is LanguageSelection -> value.displayName
                is LanguageSelectionSeparator -> "———"
                else -> "???"
            }
            append(text)
        }
    }
}
