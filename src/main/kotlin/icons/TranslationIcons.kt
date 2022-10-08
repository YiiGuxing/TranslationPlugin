package icons

import cn.yiiguxing.plugin.translate.ui.icon.SuspendIcon
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import javax.swing.Icon

object TranslationIcons {

    @JvmField
    val Translation: Icon = load("/icons/translation.svg")

    @JvmField
    val TranslationReplace: Icon = load("/icons/translationReplace.svg")

    @JvmField
    val Wordbook: Icon = load("/icons/wordbook.svg")

    @JvmField
    val Audio: Icon = load("/icons/audio.svg")

    @JvmField
    val AudioPressed: Icon = load("/icons/audioPressed.svg")

    @JvmField
    val StarOff: Icon = load("/icons/starOff.svg")

    @JvmField
    val StarOn: Icon = load("/icons/starOn.svg")

    @JvmField
    val GrayStarOff: Icon = load("/icons/grayStarOff.svg")

    @JvmField
    val AudioDisabled: Icon = load("/icons/audioDisabled.svg")

    @JvmField
    val TTSSuspend: Icon = SuspendIcon(JBColor(0x757575, 0xABABAB))

    @JvmField
    val TTSSuspendHovering: Icon = SuspendIcon(JBColor(0x555555, 0x8A8A8A))

    @JvmField
    val Swap: Icon = load("/icons/swap.svg")

    @JvmField
    val Google: Icon = load("/icons/google.svg")

    @JvmField
    val Youdao: Icon = load("/icons/youdao.svg")

    @JvmField
    val Baidu: Icon = load("/icons/baidu.svg")

    @JvmField
    val Ali: Icon = load("/icons/ali.svg")

    @JvmField
    val Deepl: Icon = load("/icons/deepl.svg")

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

    @JvmStatic
    fun load(path: String): Icon {
        return IconLoader.getIcon(path, TranslationIcons::class.java)
    }
}