package cn.yiiguxing.plugin.translate.view.utils

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsUtils
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import java.awt.Color
import java.util.*

// com.intellij.ui.jcef.JBCefScrollbarsHelper (2025.1+)
internal object JBCefScrollbarsHelper {
    private const val TRANSPARENT_CSS_COLOR = "rgba(0, 0, 0, 0.0)"

    // com.intellij.ui.components.ScrollBarPainter
    private val THUMB_OPAQUE_BACKGROUND: ColorKey =
        if (SystemInfo.isMac) key(0x33000000, 0x59808080, "ScrollBar.Mac.thumbColor")
        else key(0x33737373, 0x47A6A6A6, "ScrollBar.thumbColor")
    private val THUMB_OPAQUE_HOVERED_BACKGROUND: ColorKey =
        if (SystemInfo.isMac) key(-0x80000000, -0x737f7f80, "ScrollBar.Mac.hoverThumbColor")
        else key(0x47737373, 0x59A6A6A6, "ScrollBar.hoverThumbColor")
    private val THUMB_OPAQUE_FOREGROUND: ColorKey =
        if (SystemInfo.isMac) key(0x33000000, 0x59262626, "ScrollBar.Mac.thumbBorderColor")
        else key(0x33595959, 0x47383838, "ScrollBar.thumbBorderColor")
    private val THUMB_OPAQUE_HOVERED_FOREGROUND: ColorKey =
        if (SystemInfo.isMac) key(-0x80000000, -0x73d9d9da, "ScrollBar.Mac.hoverThumbBorderColor")
        else key(0x47595959, 0x59383838, "ScrollBar.hoverThumbBorderColor")
    private val THUMB_FOREGROUND: ColorKey =
        if (SystemInfo.isMac) key(0x00000000, 0x00262626, "ScrollBar.Mac.Transparent.thumbBorderColor")
        else key(0x33595959, 0x47383838, "ScrollBar.Transparent.thumbBorderColor")
    private val THUMB_BACKGROUND: ColorKey =
        if (SystemInfo.isMac) key(0x00000000, 0x00808080, "ScrollBar.Mac.Transparent.thumbColor")
        else key(0x33737373, 0x47A6A6A6, "ScrollBar.Transparent.thumbColor")
    private val THUMB_HOVERED_FOREGROUND: ColorKey =
        if (SystemInfo.isMac) key(-0x80000000, -0x73d9d9da, "ScrollBar.Mac.Transparent.hoverThumbBorderColor")
        else key(0x47595959, 0x59383838, "ScrollBar.Transparent.hoverThumbBorderColor")
    private val THUMB_HOVERED_BACKGROUND: ColorKey =
        if (SystemInfo.isMac) key(-0x80000000, -0x737f7f80, "ScrollBar.Mac.Transparent.hoverThumbColor")
        else key(0x47737373, 0x59A6A6A6, "ScrollBar.Transparent.hoverThumbColor")

    /**
     * Returns [scrollbars CSS code](https://developer.chrome.com/docs/css-ui/scrollbar-styling)
     * adapting the browser scrollbars look and feel to the IDE.
     *
     * This styling is based on [WebKit scrollbar styling](https://developer.chrome.com/docs/css-ui/scrollbar-styling)
     * facilities and doesn't require using third party libraries.
     */
    fun buildScrollbarsStyle(): String {
        val transparent = "rgba(0, 0, 0, 0)"

        val thumbColor = getCssColor(THUMB_OPAQUE_BACKGROUND)
        val thumbHoveredColor = getCssColor(THUMB_OPAQUE_HOVERED_BACKGROUND)
        var thumbBorderColor = getCssColor(THUMB_OPAQUE_FOREGROUND)

        if (thumbBorderColor == thumbColor) {
            // See com.intellij.ui.components.ScrollBarPainter#Thumb. In this case we ignore the borders
            thumbBorderColor = TRANSPARENT_CSS_COLOR
        }

        var thumbBorderHoveredColor = getCssColor(THUMB_OPAQUE_HOVERED_FOREGROUND)
        if (thumbBorderHoveredColor == thumbHoveredColor) {
            // See com.intellij.ui.components.ScrollBarPainter#Thumb. In this case we ignore the borders
            thumbBorderHoveredColor = TRANSPARENT_CSS_COLOR
        }

        val trackSizePx: Int = trackSizePx
        val thumbPaddingPx: Int = thumbPaddingPx
        val thumbRadiusPx: Int = thumbRadiusPx

        return String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar {
              width: %dpx;
              height: %dpx;
              background-color: %s;
            }
            
            """.trimIndent(), trackSizePx, trackSizePx, transparent
        ) + String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar-track {
              background-color: %s;
            }
            
            """.trimIndent(), transparent
        ) + String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar-track:hover {
              background-color: %s;
            }
            
            """.trimIndent(), transparent
        ) + String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar-thumb {
              background-color: %s;
              border-radius: %dpx;
              border-width: %dpx;
              border-style: solid;
              border-color: %s;
              background-clip: padding-box;
              outline: 1px solid %s;
              outline-offset: -%dpx;
            }
            
            """.trimIndent(),
            thumbColor,
            thumbRadiusPx,
            thumbPaddingPx,
            transparent,
            thumbBorderColor,
            thumbPaddingPx
        ) + String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar-thumb:hover {
              background-color: %s;
              border-radius: %dpx;
              border-width: %dpx;
              border-style: solid;
              border-color: %s;
              background-clip: padding-box;
              outline: 1px solid %s;
              outline-offset: -%dpx;
            }
            
            """.trimIndent(),
            thumbHoveredColor,
            thumbRadiusPx,
            thumbPaddingPx,
            transparent,
            thumbBorderHoveredColor,
            thumbPaddingPx
        ) + String.format(
            Locale.ROOT,
            """
            ::-webkit-scrollbar-corner {
              background-color: %s;
            }
            
            """.trimIndent(), transparent
        ) + """
            ::-webkit-scrollbar-button {
              display:none;
            }
            
            """.trimIndent()
    }


    private val trackSizePx: Int
        get() = (JBCefApp.normalizeScaledSize(if (SystemInfo.isMac) 14 else 10) * UISettingsUtils.instance.currentIdeScale).toInt()

    private val thumbPaddingPx: Int
        get() = (JBCefApp.normalizeScaledSize(if (SystemInfo.isMac) 3 else 1) * UISettingsUtils.instance.currentIdeScale).toInt()

    private val thumbRadiusPx: Int
        get() = (JBCefApp.normalizeScaledSize(if (SystemInfo.isMac) 7 else 5) * UISettingsUtils.instance.currentIdeScale).toInt()


    private fun getScrollbarAlpha(colorKey: ColorKey?): Int? {
        if (!UISettings.getInstance().useContrastScrollbars) {
            return null
        }

        val contrastElementsKeys = listOf(
            THUMB_OPAQUE_FOREGROUND,
            THUMB_OPAQUE_BACKGROUND,
            THUMB_OPAQUE_HOVERED_FOREGROUND,
            THUMB_OPAQUE_HOVERED_BACKGROUND,
            THUMB_FOREGROUND,
            THUMB_BACKGROUND,
            THUMB_HOVERED_FOREGROUND,
            THUMB_HOVERED_BACKGROUND
        )

        if (!contrastElementsKeys.contains(colorKey)) {
            return null
        }

        val lightAlpha = if (SystemInfo.isMac) 120 else 160
        val darkAlpha = if (SystemInfo.isMac) 255 else 180
        val alpha = Registry.intValue("contrast.scrollbars.alpha.level")
        if (alpha > 0) {
            return Integer.min(alpha, 255)
        }

        return if (JBColor.isBright()) lightAlpha else darkAlpha
    }

    private fun getCssColor(key: ColorKey): String {
        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
        val color = colorsScheme.getColor(key) ?: key.defaultColor
        val alpha = (getScrollbarAlpha(key) ?: color.alpha) / 255.0

        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %f)", color.red, color.green, color.blue, alpha)
    }

    private fun key(light: Int, dark: Int, name: String): ColorKey {
        return EditorColorsUtil.createColorKey(
            name,
            JBColor(
                Color(light, true),
                Color(dark, true)
            )
        )
    }
}
