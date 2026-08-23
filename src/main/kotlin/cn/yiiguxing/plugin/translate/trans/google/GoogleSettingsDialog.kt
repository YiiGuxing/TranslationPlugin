package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.openapi.ui.settings.builder.engineSettingsPanel
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.selected
import icons.TranslationIcons
import javax.swing.JComponent

class GoogleSettingsDialog : DialogWrapper(true) {

    private companion object {
        val URL_REGEX = "^https?://([^/?#\\s]+)([^?#;\\s]*)$".toRegex()
    }

    init {
        title = message("google.settings.dialog.title")
        isResizable = false

        init()
    }

    override fun isOK(): Boolean = true

    private fun String.isValidUrl(): Boolean = URL_REGEX.matches(this)

    private fun String.fixedUrl(): String? = trim().takeIf { url -> url.isValidUrl() }

    override fun createCenterPanel(): JComponent {
        val settings = service<GoogleSettings>()
        val logo = TranslationIcons.load("image/google_translate_logo.svg")
        return engineSettingsPanel(logo) {
            lateinit var serverUrlField: JBTextField
            lateinit var customServerCheckBox: JBCheckBox

            row {
                customServerCheckBox = checkBox(message("google.settings.dialog.label.custom.server"))
                    .bindSelected(
                        getter = { settings.customServer },
                        setter = { settings.customServer = it && serverUrlField.text.fixedUrl() != null }
                    )
                    .onChanged {
                        if (it.isSelected) {
                            IdeFocusManager.getInstance(null).requestFocus(serverUrlField, true)
                        }
                    }
                    .component
            }
            indent {
                row {
                    serverUrlField = textField()
                        .bindText(
                            getter = { settings.serverUrl ?: "" },
                            setter = { settings.serverUrl = it.fixedUrl() }
                        )
                        .columns(COLUMNS_LARGE)
                        .enabledIf(customServerCheckBox.selected)
                        .applyToComponent { emptyText.text = DEFAULT_GOOGLE_API_SERVER_URL }
                        .validationOnApply { textField ->
                            val serverUrl = textField.text?.takeIf { it.isNotBlank() }
                            if (customServerCheckBox.isSelected && serverUrl?.isValidUrl() == false) {
                                error(message("google.settings.dialog.error.invalid.server.url"))
                            } else null
                        }
                        .component
                }
            }

            row {
                checkBox(message("settings.documentation.translation.keep.original"))
                    .bindSelected(settings::keepOriginalDocumentation)
                    .comment(
                        comment = message("settings.documentation.translation.keep.original.description"),
                        maxLineLength = 50
                    )
            }
        }
    }
}