package cn.yiiguxing.plugin.translate.trans.google


/**
 * Data types used by the Google Translate frontend.
 *
 * Each enum constant represents a category of data that can be requested
 * or returned by the Google Translate frontend protocol.
 *
 * The [value] corresponds to the value used by Google's internal
 * `translating.frontend.TranslateDataType`.
 *
 * Note: These types are part of Google's internal frontend protocol and
 * are not part of the public Google Translate API.
 */
@Suppress("unused")
enum class GoogleTranslateDataType(val value: String) {

    /**
     * Provides the primary translation result for the input text.
     *
     * This is the main data type used for translating text from the source
     * language to the target language.
     */
    TRANSLATION("TRANSLATION"),

    /**
     * Provides alternative translations for the input text.
     *
     * Alternative translations may include different word choices,
     * phrasings, or translations that are also valid in the target language.
     */
    ALTERNATIVE_TRANSLATIONS("ALTERNATIVE_TRANSLATION"),

    /**
     * Provides full bilingual dictionary information for the input.
     *
     * This may include translations grouped by part of speech, meanings,
     * usage information, and other dictionary-related data.
     */
    BILINGUAL_DICTIONARY("BILINGUAL_DICTIONARY_FULL"),

    /**
     * Provides example sentences for the source-language query.
     *
     * These examples illustrate how the queried word or phrase is used
     * in the source language.
     */
    EXAMPLE_SENTENCE("EXAMPLE_SENTENCES_SOURCE"),

    /**
     * Provides translations that vary according to grammatical or semantic
     * gender.
     *
     * This is useful for language pairs where gender affects the form
     * of a translated word or phrase.
     */
    GENDERED_TRANSLATIONS("GENDERED_TRANSLATIONS"),

    /**
     * Identifies the language of the source text.
     *
     * This is used when the source language is automatically detected
     * rather than explicitly specified by the user.
     */
    LANGUAGE_DETECTION("LANGUAGE_IDENTIFICATION_SOURCE"),

    /**
     * Provides monolingual dictionary definitions for the queried word
     * or phrase.
     *
     * Unlike [BILINGUAL_DICTIONARY], these definitions describe the word
     * in the same language rather than translating it into another language.
     */
    MONOLINGUAL_DEFINITION("MONOLINGUAL_DEFINITIONS"),

    /**
     * Provides a correction suggestion for the user's query.
     *
     * This is typically used when the input contains a spelling or
     * query error and Google Translate can suggest a corrected form.
     */
    QUERY_CORRECTION("QUERY_CORRECTION"),

    /**
     * Automatically corrects the user's query before processing it.
     *
     * Unlike [QUERY_CORRECTION], which can provide a correction suggestion,
     * this type represents an automatically applied query correction.
     */
    QUERY_CORRECTION_AUTOCORRECT("QUERY_CORRECTION_AUTOCORRECT"),

    /**
     * Provides words related to the queried word or phrase.
     *
     * Related words are broader than strict synonyms and may include
     * semantically or morphologically related terms.
     */
    RELATED_WORDS("RELATED_WORDS"),

    /**
     * Provides the romanization of the source text.
     *
     * Romanization represents non-Latin source text using Latin characters,
     * for example, converting Japanese or Chinese text into a Latin-script
     * representation.
     */
    ROMANIZATION_SOURCE("ROMANIZATION_SOURCE"),

    /**
     * Provides the romanization of the translated text.
     *
     * This represents the target-language text using Latin characters,
     * when romanization is applicable to the target language.
     */
    ROMANIZATION_TARGET("ROMANIZATION_TARGET"),

    /**
     * Provides information about how the source text is split into sentences.
     *
     * This allows the frontend to associate individual source sentences
     * with their corresponding translation segments.
     */
    SENTENCE_SPLITS("SENTENCE_SPLITS"),

    /**
     * Provides SOS or safety-related alerts associated with the translation.
     *
     * This is a special-purpose data type used for safety or emergency-related
     * information rather than ordinary translation data.
     */
    SOS_ALERTS("SOS_ALERTS"),

    /**
     * Provides sets of synonyms for the queried word or phrase.
     *
     * Synonyms may be grouped according to meaning, usage, or other
     * linguistic relationships.
     */
    SYNONYM_SET("SYNONYM_SETS"),

}