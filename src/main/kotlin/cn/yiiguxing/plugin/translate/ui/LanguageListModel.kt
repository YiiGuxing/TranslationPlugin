package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.AppStorage
import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel

/**
 * LanguageListModel
 *
 * Created by Yii.Guxing on 2018/08/02.
 */
class LanguageListModel(languages: Collection<Lang>, selection: Lang? = null)
    : AbstractListModel<Lang>(), ComboBoxModel<Lang> {

    private val appStorage = AppStorage

    private val languageList: MutableList<Lang> = ArrayList(languages).apply { sort() }

    var selected: Lang? = selection ?: languageList.firstOrNull()
        set(value) {
            if (field != value) {
                field = value?.apply { score += 1 }
                languageList.sort()
                update()
            }
        }

    private var Lang.score: Int
        get() = if (this == Lang.AUTO) Int.MAX_VALUE else appStorage.getLanguageScore(this)
        set(value) {
            if (this != Lang.AUTO) {
                appStorage.setLanguageScore(this, value)
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