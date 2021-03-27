package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.util.ui.AnimatedIcon
import icons.Icons

internal class ProcessIcon : AnimatedIcon(
    "Querying Process", ICONS, STEP_PASSIVE, CYCLE_LENGTH
) {
    companion object {
        private const val CYCLE_LENGTH = 400

        private val ICONS = Array(9) { Icons.load("/icons/spinner/step_${it}.svg") }

        private val STEP_PASSIVE = Icons.load("/icons/spinner/step_passive.svg")
    }
}
