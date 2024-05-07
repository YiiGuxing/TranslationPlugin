@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.microsoft.models

import cn.yiiguxing.plugin.translate.BUNDLE
import cn.yiiguxing.plugin.translate.message
import com.google.gson.annotations.SerializedName
import org.jetbrains.annotations.PropertyKey

/**
 * The part-of-speech tag.
 */
internal enum class PosTag(@PropertyKey(resourceBundle = BUNDLE) displayTagKey: String) {
    @SerializedName("ADJ")
    ADJECTIVES("pos.tag.adjectives"),

    @SerializedName("ADV")
    ADVERBS("pos.tag.adverbs"),

    @SerializedName("CONJ")
    CONJUNCTIONS("pos.tag.conjunctions"),

    @SerializedName("DET")
    DETERMINERS("pos.tag.determiners"),

    @SerializedName("MODAL")
    MODAL("pos.tag.verbs"),

    @SerializedName("NOUN")
    NOUNS("pos.tag.nouns"),

    @SerializedName("PREP")
    PREPOSITIONS("pos.tag.prepositions"),

    @SerializedName("PRON")
    PRONOUNS("pos.tag.pronouns"),

    @SerializedName("VERB")
    VERBS("pos.tag.verbs"),

    @SerializedName("OTHER")
    OTHER("pos.tag.other");

    val displayTag: String by lazy { message(displayTagKey) }
}