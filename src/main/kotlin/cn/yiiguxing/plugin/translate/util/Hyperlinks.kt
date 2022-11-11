package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.ui.settings.TranslationConfigurable
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import javax.swing.event.HyperlinkEvent

object Hyperlinks {

    /**
     * A hyperlink description that opens the plugin settings page.
     *
     * @see [TranslationConfigurable]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    const val SETTINGS_DESCRIPTION = "#SETTINGS"

    /**
     * A hyperlink description that opens the configuration dialog of current translator.
     *
     * @see [Translator.checkConfiguration]
     * @see [TranslationEngine.showConfigurationDialog]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    const val TRANSLATOR_CONFIGURATION_DESCRIPTION = "#TRANSLATOR_CONFIGURATION"

    /**
     * A hyperlink description that opens the support dialog.
     *
     * @see [SupportDialog]
     */
    const val SUPPORT_DESCRIPTION = "#SUPPORT"

    /**
     * Handles default hyperlinks: [SETTINGS_DESCRIPTION],
     * [TRANSLATOR_CONFIGURATION_DESCRIPTION] and [SUPPORT_DESCRIPTION].
     *
     * @return `true` if the [hyperlink event][hyperlinkEvent] has been handled, `false` otherwise.
     */
    fun handleDefaultHyperlinkActivated(hyperlinkEvent: HyperlinkEvent): Boolean {
        when (hyperlinkEvent.description) {
            SETTINGS_DESCRIPTION -> TranslationConfigurable.showSettingsDialog()
            SUPPORT_DESCRIPTION -> SupportDialog.show()
            TRANSLATOR_CONFIGURATION_DESCRIPTION -> TranslateService.translator.checkConfiguration(true)
            else -> return false
        }

        return true
    }

}