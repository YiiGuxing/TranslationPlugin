package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBDimension
import icons.TranslationIcons
import javax.swing.JComponent

class MicrosoftTranslatorSettingsDialog : DialogWrapper(true) {

    private val settings = service<MicrosoftTranslatorSettings>()

    private val configurationPanel: DialogPanel by lazy { createConfigurationPanel() }

    init {
        title = message("microsoft.settings.dialog.title")
        isResizable = false

        init()
    }

    override fun createCenterPanel(): JComponent {
        return LogoHeaderPanel(
            TranslationIcons.load("/image/microsoft_translator_logo.svg"),
            configurationPanel
        ).apply {
            minimumSize = JBDimension(400, 100)
        }
    }

    private fun createConfigurationPanel(): DialogPanel {
        return panel {
            row {
                checkBox(message("microsoft.settings.dialog.keep.original"))
                    .bindSelected(settings::keepOriginal)
                    .comment(
                        comment = message("microsoft.settings.dialog.keep.original.experimental"),
                        maxLineLength = 50
                    )
            }
        }
    }

    override fun doOKAction() {
        configurationPanel.apply()
        super.doOKAction()
    }
}
