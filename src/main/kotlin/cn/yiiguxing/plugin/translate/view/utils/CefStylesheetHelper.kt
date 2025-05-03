package cn.yiiguxing.plugin.translate.view.utils

import cn.yiiguxing.plugin.translate.util.alphaBlend
import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.openapi.editor.colors.EditorColorsManager

object CefStylesheetHelper {

    fun buildBaseStyle(): String {
        val scheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
        val background = scheme.defaultBackground
        val foreground = scheme.defaultForeground

        val backgroundHex = background.toRGBHex()
        val foregroundHex = foreground.toRGBHex()

        val foregroundAlpha = (10..90 step 10).asSequence().map {
            "--base-color-a$it: " + foreground.alphaBlend(background, it / 100f).toRGBHex() + ";"
        }.joinToString("\n                ")

        return """
            :root {
                --base-color: $foregroundHex;
                $foregroundAlpha
                --base-background-color: $backgroundHex;
            }
            
            body {
                color: var(--base-color, $foregroundHex);
                background-color: var(--base-background-color, $backgroundHex);
            }
            
            a {
                color: var(--base-color, $foregroundHex);
                text-decoration-color: var(--base-color-a50);
            }
            
            a:hover {
                text-decoration-color: var(--base-color, $foregroundHex);
            }
        """.trimIndent()
    }
}