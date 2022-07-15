package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.TranslationStates
import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel

/**
 * LanguageListModel
 */

abstract class LanguageListModel : AbstractListModel<Lang>(), ComboBoxModel<Lang> {
    abstract var selected: Lang?

    companion object {
        fun sorted(languages: Collection<Lang>, selection: Lang? = null): LanguageListModel =
                SortedLanguageListModel(languages, selection)

        fun simple(languages: List<Lang>): LanguageListModel =
                SimpleLanguageListModel(languages)
    }
}

private class SimpleLanguageListModel(private val languages: List<Lang>) : LanguageListModel() {
    override var selected: Lang? = languages.elementAtOrNull(0)

    override fun getElementAt(index: Int): Lang = languages[index]

    override fun getSize(): Int = languages.size

    override fun setSelectedItem(anItem: Any?) {
        selected = languages.find { it == anItem }
    }

    override fun getSelectedItem(): Lang? = selected
}

private class SortedLanguageListModel(languages: Collection<Lang>, selection: Lang? = null)
    : LanguageListModel() {

    private val states = TranslationStates

    private val languageList: MutableList<Lang> = ArrayList(languages).apply { sort() }

    override var selected: Lang? = selection ?: languageList.firstOrNull()
        set(value) {
            if (field != value) {
                field = value?.apply { score += 1 }
                languageList.sort()
                update()
            }
        }

    private var Lang.score: Int
        get() = if (this == Lang.AUTO) Int.MAX_VALUE else states.getLanguageScore(this)
        set(value) {
            if (this != Lang.AUTO) {
                states.setLanguageScore(this, value)
            }
        }

    override fun getSize(): Int = languageList.size

    override fun getSelectedItem(): Any? = selected

    override fun getElementAt(index: Int): Lang = languageList[index]

    override fun setSelectedItem(anItem: Any?) {
        selected = anItem as Lang?
    }

    private fun MutableList<Lang>.sort() {
        sortByDescending { it.score }
    }

    fun update() {
        fireContentsChanged(this, -1, -1)
    }
}