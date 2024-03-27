package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.ui.AnimatedIcon
import icons.TranslationIcons
import javax.swing.Icon

private const val DELAY = 300
private val ICONS: Array<Icon> = Array(3) { TranslationIcons.load("/icons/sound/step_${it}.svg") }

class SoundIcon : AnimatedIcon(DELAY, *ICONS) {
    companion object {
        @JvmField
        val PASSIVE = ICONS[0]

        @JvmField
        val ACTIVE = TranslationIcons.load("/icons/sound/active.svg")

        @JvmField
        val DISABLED = TranslationIcons.load("/icons/sound/disabled.svg")
    }
}