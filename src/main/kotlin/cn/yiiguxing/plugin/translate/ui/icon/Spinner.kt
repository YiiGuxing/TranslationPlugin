package cn.yiiguxing.plugin.translate.ui.icon

import com.intellij.util.ui.AnimatedIcon
import com.intellij.util.ui.JBDimension
import icons.TranslationIcons
import java.awt.Dimension

internal class Spinner : AnimatedIcon("Spinner", ICONS, STEP_PASSIVE, CYCLE_LENGTH) {

    override fun calcPreferredSize(): Dimension {
        // 在高清屏幕上图标显示不全，把尺寸给加大一点点
        return JBDimension(33, 33)
    }

    companion object {
        private const val CYCLE_LENGTH = 400

        private val ICONS = Array(9) { TranslationIcons.load("/icons/spinner/step_${it}.svg") }

        private val STEP_PASSIVE = TranslationIcons.load("/icons/spinner/passive.svg")
    }
}
