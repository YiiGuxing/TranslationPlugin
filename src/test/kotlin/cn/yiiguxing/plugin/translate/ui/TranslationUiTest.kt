@file:Suppress("unused", "SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.google.*
import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.apply
import cn.yiiguxing.plugin.translate.trans.youdao.*
import cn.yiiguxing.plugin.translate.util.text.newLine

fun main() = uiTest("Translation UI Test", 500, 300/*, true*/) {
    val ui = TranslationDialogUiImpl(TranslationDialogUiProvider.testProvider())
    val mainPanel = ui.createMainPanel()

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
    ui.spellComponent.spell = "translation"
    ui.fixLangComponent.updateOnTranslation(translation)

    // setupGoogleDictDocument(ui.dictViewer, googleTranslation)
    setupYoudaoDictDocuments(ui.dictViewer, createDummyYoudaoTranslation())

    ui.showProgress()
    ui.expandDictViewer()
    // ui.showErrorPanel()

    mainPanel
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
    GSpell("translation"),
    GLanguageDetectionResult(listOf(Lang.ENGLISH), listOf(0.80f)),
    null
)

fun setupGoogleDictDocument(dictViewer: StyledViewer, googleTranslation: GoogleTranslation) {
    val document = GoogleDictDocument.Factory.getDocument(googleTranslation)
    dictViewer.apply(document!!)
}

fun setupYoudaoDictDocuments(dictViewer: StyledViewer, youdaoTranslation: YoudaoTranslation) {
    val document = YoudaoDictDocument.Factory.getDocument(youdaoTranslation)
    val webTranslationDocument = YoudaoWebTranslationDocument.Factory.getDocument(youdaoTranslation)

    document?.let {
        dictViewer.apply(it)
    }
    dictViewer.document.newLine()
    dictViewer.apply(NamedTranslationDocument(message("tip.label.webInterpretation"), webTranslationDocument!!))
}