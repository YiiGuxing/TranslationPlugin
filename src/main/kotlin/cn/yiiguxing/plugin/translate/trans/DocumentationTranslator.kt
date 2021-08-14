package cn.yiiguxing.plugin.translate.trans

interface DocumentationTranslator {

    fun translateDocumentation(documentation: String, srcLang: Lang, targetLang: Lang): BaseTranslation

}
