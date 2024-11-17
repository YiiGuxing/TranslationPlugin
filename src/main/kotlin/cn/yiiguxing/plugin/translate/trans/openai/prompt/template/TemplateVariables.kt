package cn.yiiguxing.plugin.translate.trans.openai.prompt.template

import cn.yiiguxing.plugin.translate.trans.Lang

enum class TemplateVariable(val description: String) {
    /**
     * Dollar sign (`$`). This variable is used to escape the dollar character,
     * so that it is not treated as a prefix of a template variable.
     */
    DS(
        "Dollar sign (\$). This variable is used to escape the dollar character, " +
                "so that it is not treated as a prefix of a template variable."
    ),

    /**
     * Language enum object.
     * @see LanguageEnum
     */
    LANGUAGE("Language enum object."),

    /**
     * The text to be translated.
     */
    TEXT("The text to be translated."),

    /**
     * The [source language][Lang] of the text to be translated.
     */
    SOURCE_LANGUAGE("The source language of the text to be translated."),

    /**
     * The [target language][Lang] to be translated into.
     */
    TARGET_LANGUAGE("The target language to be translated into."),
}