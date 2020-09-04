package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.text.GoogleDictDocument
import cn.yiiguxing.plugin.translate.trans.text.setup
import com.intellij.ide.ui.laf.IntelliJLaf
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

    val frame = JFrame("dialog test");

    val ui = NewTranslationDialogUiImpl(NewTranslationDialogUiProvider.testProvider())
    val panel = ui.createMainPanel()

    ui.sourceLangComboBox.model = LanguageListModel.simple(listOf(Lang.AUTO, Lang.ENGLISH, Lang.CHINESE))
    ui.targetLangComboBox.model = LanguageListModel.simple(listOf(Lang.CHINESE, Lang.ENGLISH))

    ui.initFonts(UI.FontPair(UI.defaultFont, UI.defaultFont.lessOn(2f)))

    ui.inputTextArea.text = "translaton"
    ui.translationTextArea.text = "翻译"
    ui.detectedLanguageLabel.text = "English"
    ui.srcTransliterationLabel.text = "transˈlāSH(ə)n"
    ui.targetTransliterationLabel.text = "Fānyì"

    val googleTranslation = GoogleTranslation("translation",
        Lang.ENGLISH,
        Lang.CHINESE,
        listOf(GTransSentence("translation", "翻译", 2), GTranslitSentence("transˈlāSH(ə)n", "Fānyì")),
        listOf(
            GDict("noun",
                listOf("翻译", "翻", "解答"),
                listOf(
                    GDictEntry("翻译", listOf("translation", "interpretation", "rendering", "rendition", "version", "decipherment"), 0f),
                    GDictEntry("翻", listOf("turn", "translation", "turnover"), 0f),
                    GDictEntry("解答", listOf("answer", "solution", "explanation", "response", "reply", "translation"), 0f)
                )
            )
        ),
        GSpell(""),
        GLDResult(listOf(Lang.ENGLISH), listOf(0.80f)),
        null
    )

    val document = GoogleDictDocument.Factory.getDocument(googleTranslation)
    ui.dictViewer.setup(document!!)
    ui.expandDictViewer()

//    ui.spellComponent.spell = "translation"
    val translation = googleTranslation.toTranslation().copy(srcLang = Lang.CHINESE)
    ui.fixLangComponent.updateOnTranslation(translation)

    frame.size = JBDimension(600, 500)
    frame.contentPane = panel
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}