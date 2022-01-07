package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.util.ui.AnimatedIcon
import icons.Icons

class SmallProgressIcon : AnimatedIcon("Spinner", ICONS, STEP_PASSIVE, CYCLE_LENGTH) {

    companion object {
        private const val CYCLE_LENGTH = 400

        private val ICONS = Array(12) { Icons.load("/icons/progress/small/step_${it}.svg") }

        private val STEP_PASSIVE = Icons.load("/icons/progress/small/step_0.svg")
    }
}