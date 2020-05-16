/*
 * Extensions
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBFont
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.JComboBox
import javax.swing.JComponent


@Suppress("NOTHING_TO_INLINE")
inline fun JComponent.andTransparent() = apply { isOpaque = false }

@Suppress("NOTHING_TO_INLINE")
inline fun JComponent.andOpaque() = apply { isOpaque = true }

/**
 * 当前选中项
 */
inline var <reified E> JComboBox<E>.selected: E?
    get() = selectedItem as? E
    set(value) {
        selectedItem = value
    }

/**
 * Returns the width of the [Dimension].
 */
operator fun Dimension.component1() = width

/**
 * Returns the height of the [Dimension].
 */
operator fun Dimension.component2() = height

/**
 * Returns a new color with [alpha].
 *
 * @param alpha the alpha(0.0f ~ 1.0f).
 */
fun Color.withAlpha(alpha: Float) = toAlpha((0xFF * alpha).toInt())

/**
 * Returns a new color with [alpha].
 *
 * @param alpha the alpha(0 ~ 255).
 */
fun Color.toAlpha(alpha: Int) = Color(red, green, blue, alpha)

/**
 * Creates a new [Font][JBFont] object by replicating this [Font] object
 * and applying a new style and [scaled size][size].
 */
fun Font.deriveScaledFont(style: Int, size: Float)
        : JBFont = JBFont.create(deriveFont(style, JBUIScale.scale(size)), false)

/**
 * Creates a new [Font][JBFont] object by replicating the current
 * [Font] object and applying a new [scaled size][size] to it.
 */
fun Font.deriveScaledFont(size: Float): JBFont = JBFont.create(deriveFont(JBUIScale.scale(size)), false)