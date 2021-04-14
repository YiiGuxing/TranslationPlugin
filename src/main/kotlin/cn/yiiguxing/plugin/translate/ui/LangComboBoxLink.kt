package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Point
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

class LangComboBoxLink : LinkLabel<Lang>("Empty", AllIcons.General.ButtonDropTriangle) {

    private var currentPopup: JBPopup? = null

    val isPopupShowing: Boolean get() = currentPopup != null

    var model: LanguageListModel = LanguageListModel.simple(listOf())
        set(value) {
            field = value
            onItemChosen(value.selected)
        }

    private val listeners: MutableList<(Lang?, Lang?, Boolean) -> Unit> = arrayListOf()

    init {
        horizontalTextPosition = SwingConstants.LEADING
        myPaintUnderline = false
        border = emptyBorder(0, leftAndRight = 5)
    }

    fun togglePopup() {
        if (currentPopup?.cancel() == null) {
            doClick()
        }
    }

    override fun getTextColor(): Color {
        return JBUI.CurrentTheme.Label.foreground()
    }

    fun onItemChosen(lang: Lang?, fromUser: Boolean = false) {
        val oldLang = selected
        setListener(LangComboBoxLinkListener, lang)
        text = lang?.langName ?: "No language"
        if (lang != oldLang) {
            model.selected = lang
        }
        listeners.forEach { it(lang, oldLang, fromUser) }
    }

    fun addItemListener(listener: (newLang: Lang?, oldLang: Lang?, fromUser: Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun languages(): List<Lang> {
        return IntStream.range(0, model.size)
            .mapToObj { idx -> model.getElementAt(idx) }
            .collect(Collectors.toList())
    }

    inline var selected: Lang?
        get() = linkData
        set(value) {
            onItemChosen(value)
        }

    private object LangComboBoxLinkListener : LinkListener<Lang> {
        override fun linkSelected(source: LinkLabel<Lang>, lang: Lang) {
            val langLink = source as LangComboBoxLink
            if (langLink.isPopupShowing) {
                return
            }

            JBPopupFactory
                .getInstance()
                .createPopupChooserBuilder(langLink.languages())
                .setVisibleRowCount(8)
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemChosenCallback { langLink.onItemChosen(it, true) }
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
                .also { popup ->
                    Disposer.register(popup) { langLink.currentPopup = null }
                    popup.show(source)
                    langLink.currentPopup = popup
                }
        }
    }
}

