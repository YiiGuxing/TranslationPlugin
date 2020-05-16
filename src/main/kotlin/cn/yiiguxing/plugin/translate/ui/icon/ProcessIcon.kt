package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.AnimatedIcon

internal class ProcessIcon : AnimatedIcon(
    "Querying Process", ICONS, STEP_PASSIVE, CYCLE_LENGTH
) {
    companion object {
        private const val CYCLE_LENGTH = 400

        private val ICONS = Array(9) { IconLoader.getIcon("/icons/spinner/step_${it}.png") }

        private val STEP_PASSIVE = IconLoader.getIcon("/icons/spinner/step_passive.png")
    }
}
