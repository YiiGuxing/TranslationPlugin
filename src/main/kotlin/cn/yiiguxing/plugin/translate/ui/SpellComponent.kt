package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.TranslationIcons
import javax.swing.JLabel
import javax.swing.SwingConstants

class SpellComponent : BorderLayoutPanel() {
    var spell: String? = null
        set(value) {
            field = value
            isVisible = spell != null
            spellText.text = spell
            spellText.toolTipText = spell
        }

    val spellLabel = JLabel(
        message("translation.ui.pane.label.spell"),
        TranslationIcons.AutoAwesome,
        SwingConstants.LEADING
    )

    val spellText = ActionLink("") {
        val handler = onSpellFixedHandler ?: return@ActionLink
        spell?.let { handler(it) }
    }

    private var onSpellFixedHandler: ((String) -> Unit)? = null

    init {
        isOpaque = false
        isVisible = false
        addToLeft(spellLabel)
        addToCenter(spellText)
        spellLabel.border = JBUI.Borders.emptyRight(5)
    }

    fun onSpellFixed(handler: (spell: String) -> Unit) {
        onSpellFixedHandler = handler
    }
}