package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.form.SettingsForm
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.ui.laf.darcula.DarculaLaf
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import com.intellij.util.ui.JBDimension
import javax.swing.JFrame
import javax.swing.UIManager

fun main() {
    IconLoader.activate()
    IconManager.activate()
    val laf = IntelliJLaf()
//    val laf = DarculaLaf()
    UIManager.setLookAndFeel(laf)

    val frame = JFrame("Settings")
    val panel = object : SettingsForm() {}.createMainPanel()
    panel.border = emptyBorder(11, 16)

    frame.size = JBDimension(650, 900)
    frame.contentPane = panel
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}
