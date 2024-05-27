package cn.yiiguxing.plugin.translate.compat.ui

import com.intellij.ui.GotItTooltip
import javax.swing.JComponent

enum class GotItTooltipPosition {
    TOP, RIGHT, BOTTOM, LEFT
}

fun GotItTooltip.show(component: JComponent, position: GotItTooltipPosition) {
    val positionProvider = when (position) {
        GotItTooltipPosition.TOP -> GotItTooltip.TOP_MIDDLE
        GotItTooltipPosition.LEFT -> GotItTooltip.LEFT_MIDDLE
        GotItTooltipPosition.RIGHT -> GotItTooltip.RIGHT_MIDDLE
        GotItTooltipPosition.BOTTOM -> GotItTooltip.BOTTOM_MIDDLE
    }
    show(component, positionProvider)
}