package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Panel with a logo header (Recommended logo height is 30px).
 * @param logo The logo
 */
class LogoHeaderPanel(logo: Icon) : JBPanel<LogoHeaderPanel>(layout()) {

    init {
        add(createLogoPane(logo))
    }

    /**
     * Panel with a logo header (Recommended logo height is 30px).
     * @param logo The logo.
     * @param content The content pane.
     */
    constructor(logo: Icon, content: JComponent) : this(logo) {
        add(content)
    }

    companion object {
        private fun layout() = VerticalLayout(JBUIScale.scale(8))

        private fun createLogoPane(logo: Icon): JComponent {
            return JLabel(logo).apply {
                border = JBUI.Borders.empty(24, 0, 32, 0)
            }
        }
    }
}