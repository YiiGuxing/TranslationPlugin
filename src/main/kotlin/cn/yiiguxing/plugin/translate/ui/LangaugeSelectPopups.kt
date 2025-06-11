package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory

object LangaugeSelectPopups {

    fun createPopup(ui: LangaugeSelectPopupUI): JBPopup {
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(ui.component, ui.sourceLangComboBox)
            .setResizable(false)
            .setMovable(true)
            .setModalContext(true)
            .setRequestFocus(true)
            .createPopup()
        ui.setPopup(popup)

        return popup
    }

}