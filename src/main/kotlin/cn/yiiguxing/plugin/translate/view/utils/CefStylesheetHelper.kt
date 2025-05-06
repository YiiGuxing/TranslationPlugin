package cn.yiiguxing.plugin.translate.view.utils

import cn.yiiguxing.plugin.translate.util.alphaBlend
import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor

object CefStylesheetHelper {

    fun buildBaseStyle(): String {
        val scheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
        val background = scheme.defaultBackground
        val foreground = JBColor(0x19191C, 0xFFFFFF)

        val backgroundHex = background.toRGBHex()
        val foregroundHex = foreground.toRGBHex()

        val foregroundAlpha = (10..90 step 10).asSequence().map {
            "--base-color-a$it: " + foreground.alphaBlend(background, it / 100f).toRGBHex() + ";"
        }.joinToString("\n                ")

        val linkColor = JBColor(0x0269E1, 0x56A8F5).toRGBHex()

        return """
            :root {
                --base-color: $foregroundHex;
                $foregroundAlpha
                --base-background-color: $backgroundHex;
                --base-link-color: $linkColor;
            }
            
            body {
                color: var(--base-color, $foregroundHex);
                background-color: var(--base-background-color, $backgroundHex);
            }
            
            a {
                color: var(--base-link-color, $linkColor);
                text-decoration: none;
            }
            
            a:hover {
                text-decoration: underline;
            }
        """.trimIndent()
    }
}