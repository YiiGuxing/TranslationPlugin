package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.ide.ui.laf.IntelliJLaf
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import com.intellij.util.ui.JBDimension
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.UIManager

fun main() {
    IconLoader.activate()
    IconManager.activate()
    val laf = IntelliJLaf()
//    val laf = DarculaLaf()
    UIManager.setLookAndFeel(laf)

    val frame = JFrame("dialog test");

    val ui = NewTranslationDialogUiImpl(NewTranslationDialogUiProvider.testProvider())
    ui.sourceLangComboBox.model = LanguageListModel.simple(listOf(Lang.AUTO, Lang.ENGLISH, Lang.CHINESE))
    ui.targetLangComboBox.model = LanguageListModel.simple(listOf(Lang.CHINESE, Lang.ENGLISH))

    ui.initFonts(UI.FontPair(UI.defaultFont, UI.defaultFont.lessOn(2f)))

    ui.inputTextArea.text = "translation"
    ui.translationTextArea.text = "翻译"
    ui.detectedLanguageLabel.text = "English"
    ui.srcTransliterationLabel.text = "transˈlāSH(ə)n"
    ui.targetTransliterationLabel.text = "Fānyì"

    val panel = ui.createMainPanel()

    frame.size = JBDimension(600, 500)
    frame.contentPane.layout = BorderLayout()
    frame.contentPane.add(panel, BorderLayout.NORTH)
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}