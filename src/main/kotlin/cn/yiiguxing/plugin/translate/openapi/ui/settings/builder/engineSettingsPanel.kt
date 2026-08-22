package cn.yiiguxing.plugin.translate.openapi.ui.settings.builder

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import javax.swing.Icon

/**
 * Creates a [DialogPanel] for a translation engine's settings, with a
 * centered [logo] displayed at the top followed by the content built by [builder].
 *
 * @param logo The engine logo shown at the top of the panel.
 * @param builder The panel builder used to construct the settings content.
 * @return The constructed settings panel.
 */
inline fun engineSettingsPanel(logo: Icon, crossinline builder: Panel.() -> Unit): DialogPanel {
    return panel {
        row {
            icon(logo).align(AlignX.CENTER).customize(UnscaledGaps(top = 24, bottom = 32))
        }

        builder()
    }
}