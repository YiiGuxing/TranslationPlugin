package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.settings.SettingsUi
import com.intellij.ui.components.JBScrollPane

fun main() = uiTest("Settings UI Test", 650, 900/*, true*/) {
    val settingsUi = object : SettingsUi() {
        override fun isSupportDocumentTranslation(): Boolean = true
    }
    val panel = settingsUi.createMainPanel()
    panel.border = emptyBorder(11, 16)
    JBScrollPane(panel)
}
