package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.util.ui.AnimatedIcon
import icons.TranslationIcons

class SmallProgressIcon : AnimatedIcon("SmallProgress", ICONS, STEP_PASSIVE, CYCLE_LENGTH) {

    companion object {
        private const val CYCLE_LENGTH = 600

        private val ICONS = Array(12) { TranslationIcons.load("/icons/progress/small/step_${it}.svg") }

        private val STEP_PASSIVE = TranslationIcons.load("/icons/progress/small/passive.svg")
    }
}