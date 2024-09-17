package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object TranslationIcons {

    @JvmField
    val Logo: Icon = load("/icons/logo.svg")

    @JvmField
    val Translation: Icon = load("/icons/translation.svg")

    @JvmField
    val TranslationInactivated: Icon = load("/icons/translationInactivated.svg")

    @JvmField
    val TranslationReplace: Icon = load("/icons/translationReplace.svg")

    @JvmField
    val Wordbook: Icon = load("/icons/wordbook.svg")

    @JvmField
    val StarOff: Icon = load("/icons/starOff.svg")

    @JvmField
    val StarOn: Icon = load("/icons/starOn.svg")

    @JvmField
    val GrayStarOff: Icon = load("/icons/grayStarOff.svg")

    @JvmField
    val Stop: Icon = load("/icons/stop.svg")

    @JvmField
    val Swap: Icon = load("/icons/swap.svg")

    @JvmField
    val Support: Icon = load("/icons/support.svg")

    @JvmField
    val AutoAwesome: Icon = load("/icons/autoAwesome.svg")

    @JvmField
    val ArrowDownExpand: Icon = load("/icons/arrowDownExpand.svg")

    @JvmField
    val ArrowUpCollapse: Icon = load("/icons/arrowUpCollapse.svg")

    @JvmField
    val Lightning: Icon = load("/icons/lightning.svg")

    @JvmField
    val Documentation: Icon = load("/icons/documentation.svg")

    @JvmField
    val Quote: Icon = load("/icons/quote.svg")


    /** Translation engine logos. */
    object Engines {
        @JvmField
        val Ali: Icon = load("/icons/engines/ali.svg")

        @JvmField
        val Baidu: Icon = load("/icons/engines/baidu.svg")

        @JvmField
        val Deepl: Icon = load("/icons/engines/deepl.svg")

        @JvmField
        val Google: Icon = load("/icons/engines/google.svg")

        @JvmField
        val Microsoft: Icon = load("/icons/engines/microsoft.svg")

        @JvmField
        val OpenAI: Icon = load("/icons/engines/openai.svg")

        @JvmField
        val Youdao: Icon = load("/icons/engines/youdao.svg")
    }


    @JvmStatic
    fun load(path: String): Icon {
        return IconLoader.getIcon(path, TranslationIcons::class.java)
    }
}