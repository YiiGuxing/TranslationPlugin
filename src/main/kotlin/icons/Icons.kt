package icons

import cn.yiiguxing.plugin.translate.ui.icon.SuspendIcon
import com.intellij.ui.IconManager
import com.intellij.ui.JBColor
import javax.swing.Icon

object Icons {

    @JvmField
    val Translation: Icon = load("/icons/translation.svg")

    @JvmField
    val TranslationReplace: Icon = load("/icons/translationReplace.svg")

    @JvmField
    val Wordbook: Icon = load("/icons/wordbook.svg")

    @JvmField
    val Pin: Icon = load("/icons/pin.svg")

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
    val Detail: Icon = load("/icons/detail.svg")

    @JvmField
    val AudioDisabled: Icon = load("/icons/audioDisabled.svg")

    @JvmField
    val TTSSuspend: Icon = SuspendIcon(JBColor(0x757575, 0xABABAB))

    @JvmField
    val TTSSuspendHovering: Icon = SuspendIcon(JBColor(0x555555, 0x8A8A8A))

    @JvmField
    val Swap: Icon = load("/icons/swap.svg")

    @JvmField
    val Swap2: Icon = load("/icons/swap2.svg")

    @JvmField
    val SwapHovering: Icon = load("/icons/swapHovering.svg")

    @JvmField
    val SwapDisabled: Icon = load("/icons/swapDisabled.svg")

    @JvmField
    val ClearText: Icon = load("/icons/clearText.svg")

    @JvmField
    val ClearTextHovering: Icon = load("/icons/clearTextHovering.svg")

    @JvmField
    val ClearTextDisabled: Icon = load("/icons/clearTextDisabled.svg")

    @JvmField
    val CopyAll: Icon = load("/icons/copy.svg")

    @JvmField
    val CopyAllHovering: Icon = load("/icons/copyHovering.svg")

    @JvmField
    val CopyAllDisabled: Icon = load("/icons/copyDisabled.svg")

    @JvmField
    val CopyToClipboard: Icon = load("/icons/copyToClipboard.svg")

    @JvmField
    val Google: Icon = load("/icons/google.svg")

    @JvmField
    @Suppress("SpellCheckingInspection")
    val Youdao: Icon = load("/icons/youdao.png")

    @JvmField
    @Suppress("SpellCheckingInspection")
    val Baidu: Icon = load("/icons/baidu.svg")

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

    @JvmStatic
    fun load(path: String): Icon {
        return IconManager.getInstance().getIcon(path, Icons::class.java)
    }
}