package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import com.intellij.ui.components.JBScrollPane

fun main() = uiTest("Settings UI Test", 650, 900/*, true*/) {
    val settingsForm = object : SettingsForm() {
        override fun isSupportDocumentTranslation(): Boolean = true
    }
    val panel = settingsForm.createMainPanel()
    panel.border = emptyBorder(11, 16)
    JBScrollPane(panel)
}
