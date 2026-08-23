package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.openapi.ui.settings.builder.engineSettingsPanel
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.ui.JBDimension
import icons.TranslationIcons
import javax.swing.JComponent

class MicrosoftTranslatorSettingsDialog : DialogWrapper(true) {

    init {
        title = message("microsoft.settings.dialog.title")
        isResizable = false

        init()
    }

    override fun isOK(): Boolean = true

    override fun createCenterPanel(): JComponent {
        val settings = service<MicrosoftTranslatorSettings>()
        val logo = TranslationIcons.load("/image/microsoft_translator_logo.svg")
        return engineSettingsPanel(logo) {
            row {
                checkBox(message("settings.documentation.translation.keep.original"))
                    .bindSelected(settings::keepOriginalDocumentation)
                    .comment(
                        comment = message("settings.documentation.translation.keep.original.description"),
                        maxLineLength = 50
                    )
            }
        }.apply {
            minimumSize = JBDimension(400, 100)
        }
    }
}
