package cn.yiiguxing.plugin.translate.util

import java.awt.Color


@Suppress("UseJBColor")
fun Color.alphaBlend(background: Color, alpha: Float): Color {
    require(alpha in 0.0..1.0) { "alpha(0.0 <= alpha <= 1.0): $alpha" }

    val r = (1 - alpha) * background.red + alpha * red
    val g = (1 - alpha) * background.green + alpha * green
    val b = (1 - alpha) * background.blue + alpha * blue

    return Color(r.toInt(), g.toInt(), b.toInt())
}

fun Color.toRGBHex(): String = "#" + (rgb and 0xffffff).toString(16).padStart(6, '0')