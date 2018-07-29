package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import javax.swing.Icon


/**
 * 图标
 */
object Icons {

    val Translate: Icon = IconLoader.getIcon("/icons/translate.png")
    val Translate2: Icon = IconLoader.getIcon("/icons/translate2.png")
    val Pin: Icon = IconLoader.getIcon("/icons/pin.png")
    val Copy: Icon = AllIcons.Actions.Copy
    val Close: Icon = IconLoader.getIcon("/icons/close.png")
    val ClosePressed: Icon = IconLoader.getIcon("/icons/closePressed.png")
    val Audio: Icon = IconLoader.getIcon("/icons/audio.png")
    val AudioPressed: Icon = IconLoader.getIcon("/icons/audioPressed.png")
    val AudioDisabled: Icon = IconLoader.getIcon("/icons/audioDisabled.png")
    val TTSSuspend: Icon = SuspendIcon(JBColor(0x757575, 0xABABAB))
    val TTSSuspendHovering: Icon = SuspendIcon(JBColor(0x555555, 0x8A8A8A))
    val Swap: Icon = IconLoader.getIcon("/icons/swap.png")
    val Swap2: Icon = IconLoader.getIcon("/icons/swap2.png")
    val SwapHovering: Icon = IconLoader.getIcon("/icons/swapHovering.png")
    val SwapDisabled: Icon = IconLoader.getIcon("/icons/swapDisabled.png")
    val ClearText: Icon = IconLoader.getIcon("/icons/clearText.png")
    val ClearTextHovering: Icon = IconLoader.getIcon("/icons/clearTextHovering.png")
    val ClearTextDisabled: Icon = IconLoader.getIcon("/icons/clearTextDisabled.png")
    val CopyAll: Icon = IconLoader.getIcon("/icons/copy.png")
    val CopyAllHovering: Icon = IconLoader.getIcon("/icons/copyHovering.png")
    val CopyAllDisabled: Icon = IconLoader.getIcon("/icons/copyDisabled.png")
    val CopyToClipboard: Icon = IconLoader.getIcon("/icons/copyToClipboard.png")
    val Google: Icon = IconLoader.getIcon("/icons/google.png")
    val Youdao: Icon = IconLoader.getIcon("/icons/youdao.png")
    val Baidu: Icon = IconLoader.getIcon("/icons/baidu.png")

}
