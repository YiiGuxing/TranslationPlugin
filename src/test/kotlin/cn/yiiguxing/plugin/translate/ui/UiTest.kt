package cn.yiiguxing.plugin.translate.ui

import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.ide.ui.laf.darcula.DarculaLaf
import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.JBDimension
import java.awt.Container
import javax.swing.JFrame
import javax.swing.UIManager

internal fun uiTest(
    windowTitle: String,
    windowWidth: Int,
    windowHeight: Int,
    darcula: Boolean = false,
    uiProvider: (() -> Container)
) {
    IconLoader.activate()
    val laf = if (darcula) DarculaLaf() else IntelliJLaf()
    UIManager.setLookAndFeel(laf)

    val frame = JFrame(windowTitle)
    frame.contentPane = uiProvider()
    frame.size = JBDimension(windowWidth, windowHeight)
    frame.setLocationRelativeTo(null)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isVisible = true
}
