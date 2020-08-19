package cn.yiiguxing.plugin.translate.ui.icon

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.ui.LanguageListModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Point
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

class LangComboBoxLink : LinkLabel<Lang>("Empty", AllIcons.General.ButtonDropTriangle) {

    var model: LanguageListModel = LanguageListModel.simple(listOf())
        set(value) {
            field = value
            onItemChosen(value.selected)
        }

    private val listeners: MutableList<(Lang?) -> Unit> = arrayListOf()

    init {
        horizontalTextPosition = SwingConstants.LEADING
        myPaintUnderline = false
    }

    override fun getForeground(): Color {
        return UIUtil.getLabelForeground()
    }

    fun onItemChosen(lang: Lang?) {
        setListener(LangComboBoxLinkListener, lang)
        text = lang?.langName ?: "No language"
        listeners.forEach { it(lang) }
    }

    fun addItemListener(listener: (Lang?) -> Unit) {
        listeners.add(listener)
    }

    fun languages(): List<Lang> {
        return IntStream.range(0, model.size)
                .mapToObj { idx -> model.getElementAt(idx) }
                .collect(Collectors.toList())
    }

    inline var selected: Lang
        get() = linkData
        set(value) {
            onItemChosen(value)
        }

    private object LangComboBoxLinkListener : LinkListener<Lang> {
        override fun linkSelected(source: LinkLabel<*>, lang: Lang) {
            val langLink = source as LangComboBoxLink
            val list = langLink.languages()

            JBPopupFactory.getInstance().createPopupChooserBuilder(list)
                    .setVisibleRowCount(8)
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                    .setItemChosenCallback { langLink.onItemChosen(it) }
                    .setRenderer(SimpleListCellRenderer.create { label, value: Lang, _ ->
                        label.text = value.langName
                        label.border = langLink.border
                        label.font = langLink.font
                    })
                    .addListener(object : JBPopupListener {
                        override fun beforeShown(event: LightweightWindowEvent) {
                            val popup = event.asPopup()
                            val relativePoint = RelativePoint(langLink, Point(0, langLink.height))
                            val screenPoint = Point(relativePoint.screenPoint)

                            popup.setLocation(screenPoint)
                        }
                    })
                    .createPopup()
                    .show(source)
        }
    }
}

