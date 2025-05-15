package cn.yiiguxing.plugin.translate.view.utils

import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.ui.JBColor

object CefStylesheetHelper {
    val BACKGROUND = JBColor(0xFFFFFF, 0x19191C)
    val BASE_COLOR = JBColor(0x19191C, 0xDFE1E6)
    val PRIMARY_COLOR = JBColor(0x000000, 0xFFFFFF)
    val LINK_COLOR = JBColor(0x0000EE, 0x2196F3)
    val BUTTON_COLOR = JBColor(0xFFFFFF, 0xFFFFFF)
    val BUTTON_BG_COLOR = JBColor(0x0047FD, 0x0047FD)
    val BUTTON_HOVER_BG_COLOR = JBColor(0x053ED0, 0x053ED0)
    val CARD_BG_COLOR = JBColor(0xF7F7F7, 0x3C3F41)

    fun buildBaseStyle(): String {
        val baseColorAlpha = (10..90 step 10).asSequence().map {
            buildAlphaProperty("--base-color", BASE_COLOR, it)
        }.joinToString("\n                ")
        val primaryColorAlpha = (10..90 step 10).asSequence().map {
            buildAlphaProperty("--primary-color", PRIMARY_COLOR, it)
        }.joinToString("\n                ")

        val backgroundHex = BACKGROUND.toRGBHex()
        val baseColorHex = BASE_COLOR.toRGBHex()
        val linkColorHex = LINK_COLOR.toRGBHex()

        return """
            :root {
                --base-color: $baseColorHex;
                $baseColorAlpha
                --primary-color: ${PRIMARY_COLOR.toRGBHex()};
                $primaryColorAlpha
                --base-background-color: $backgroundHex;
                --base-link-color: $linkColorHex;
                --base-button-color: ${BUTTON_COLOR.toRGBHex()};
                --base-button-bg-color: ${BUTTON_BG_COLOR.toRGBHex()};
                --base-button-hover-bg-color: ${BUTTON_HOVER_BG_COLOR.toRGBHex()};
                --base-card-bg-color: ${CARD_BG_COLOR.toRGBHex()};
            }
            
            body {
                color: var(--base-color, $baseColorHex);
                background-color: var(--base-background-color, $backgroundHex);
            }
            
            a {
                color: var(--base-link-color, $linkColorHex);
                text-decoration: none;
            }
            
            a:hover {
                text-decoration: underline;
            }
        """.trimIndent()
    }

    private fun JBColor.toRGBA(alpha: Float): String {
        return "rgba(${red}, ${green}, ${blue}, ${alpha.coerceIn(0f..1f)})"
    }

    private fun buildAlphaProperty(name: String, color: JBColor, transparency: Int): String {
        check(transparency in 0..100)
        return "$name-a$transparency: ${color.toRGBA(transparency / 100f)};"
    }
}