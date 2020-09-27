package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.text.*
import cn.yiiguxing.plugin.translate.util.text.newLine
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
    // val laf = DarculaLaf()
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

    val googleTranslation = createGoogleTranslation()
    val translation = googleTranslation.toTranslation().copy(srcLang = Lang.CHINESE)
    // ui.spellComponent.spell = "translation"
    ui.fixLangComponent.updateOnTranslation(translation)

    // setupGoogleDictDocument(ui.dictViewer, googleTranslation)
    setupYoudaoDictDocuments(ui.dictViewer, createDummyYoudaoTranslation())

    ui.expandDictViewer()
    frame.size = JBDimension(400, 300)
    frame.contentPane = panel
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}

fun createDummyYoudaoTranslation(): YoudaoTranslation = YoudaoTranslation(
    "query", -1, null,
    arrayOf("translation1", "translation2", "translation3"),
    YBasicExplain(
        "dummy",
        "dummy",
        "dummy",
        arrayOf("explanation1", "explanation2"),
        arrayOf(YWordFormWrapper(YWordForm("form", "value")))
    ),
    "English",
    arrayOf(YWebExplain("key1", arrayOf("value1", "value2")), YWebExplain("key2", arrayOf("value3")))
)

fun createGoogleTranslation(): GoogleTranslation = GoogleTranslation(
    "translation",
    Lang.ENGLISH,
    Lang.CHINESE,
    listOf(GTransSentence("translation", "翻译", 2), GTranslitSentence("transˈlāSH(ə)n", "Fānyì")),
    listOf(
        GDict(
            "noun",
            listOf("翻译", "翻", "解答"),
            listOf(
                GDictEntry(
                    "翻译",
                    listOf("translation", "interpretation", "rendering", "rendition", "version", "decipherment"),
                    0f
                ),
                GDictEntry("翻", listOf("turn", "translation", "turnover"), 0f),
                GDictEntry("解答", listOf("answer", "solution", "explanation", "response", "reply", "translation"), 0f)
            )
        )
    ),
    GSpell(""),
    GLDResult(listOf(Lang.ENGLISH), listOf(0.80f)),
    null
)

fun setupGoogleDictDocument(dictViewer: StyledViewer, googleTranslation: GoogleTranslation) {
    val document = GoogleDictDocument.Factory.getDocument(googleTranslation)
    dictViewer.setup(document!!)
}

fun setupYoudaoDictDocuments(dictViewer: StyledViewer, youdaoTranslation: YoudaoTranslation) {
    val document = YoudaoDictDocument.Factory.getDocument(youdaoTranslation)
    val webTranslationDocument = YoudaoWebTranslationDocument.Factory.getDocument(youdaoTranslation)

    dictViewer.setup(document)
    dictViewer.document.newLine()
    dictViewer.setup(NamedTranslationDocument(message("tip.label.webInterpretation"), webTranslationDocument!!))
}