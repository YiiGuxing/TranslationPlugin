package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.LanguageSelection
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.GroupedComboBoxRenderer


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

    private var separatorItem: Lang? = null

    init {
        isSwingPopup = false
        renderer = Renderer()
    }

    fun setLanguages(languages: List<Lang>) {
        val languageSelections = LanguageSelection.entries
        val items = ArrayList<Any>(languageSelections.size + languages.size)
        items.addAll(LanguageSelection.entries)
        items.addAll(languages)

        separatorItem = languages.firstOrNull()
        model = object : CollectionComboBoxModel<Any>(items, fixSelection(items, selectedItem)) {
            override fun setSelectedItem(item: Any?) {
                if (item is Lang || item is LanguageSelection) {
                    super.setSelectedItem(fixSelection(internalList, item))
                }
            }
        }
    }

    private companion object {
        fun fixSelection(items: List<Any?>, item: Any?): Any {
            return item?.takeIf { it in items }
                ?: Lang.AUTO.takeIf { it in items }
                ?: LanguageSelection.MAIN_OR_ENGLISH
        }
    }

    private inner class Renderer : GroupedComboBoxRenderer<Any>() {
        private val separator = ListSeparator(message("language.selection.combo.box.languages"))

        override fun getText(item: Any): String = when (item) {
            is Lang -> item.localeName
            is LanguageSelection -> item.displayName
            else -> ""
        }

        override fun separatorFor(value: Any): ListSeparator? =
            separator.takeIf { separatorItem != null && value === separatorItem }
    }
}
