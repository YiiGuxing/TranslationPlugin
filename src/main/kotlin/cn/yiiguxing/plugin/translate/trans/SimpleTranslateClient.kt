package cn.yiiguxing.plugin.translate.trans


typealias TranslateCall = (text: String, srcLang: Lang, targetLang: Lang) -> String
typealias TranslationParser<T> = (translation: String, original: String, srcLang: Lang, targetLang: Lang) -> T


class SimpleTranslateClient<T : BaseTranslation>(
    translator: Translator,
    private val call: TranslateCall,
    private val parser: TranslationParser<T>
) : TranslateClient<T>(translator) {

    override fun doExecute(text: String, srcLang: Lang, targetLang: Lang): String {
        return call(text, srcLang, targetLang)
    }

    override fun parse(translation: String, original: String, srcLang: Lang, targetLang: Lang): T {
        return parser(translation, original, srcLang, targetLang)
    }
}