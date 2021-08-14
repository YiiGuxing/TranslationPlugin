package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SETTINGS
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION
import cn.yiiguxing.plugin.translate.ui.settings.OptionsConfigurable
import cn.yiiguxing.plugin.translate.util.TranslateService
import javax.swing.event.HyperlinkEvent

object DefaultHyperlinkHandler {

    fun handleHyperlinkActivated(hyperlinkEvent: HyperlinkEvent): Boolean {
        when (hyperlinkEvent.description) {
            HTML_DESCRIPTION_SETTINGS -> OptionsConfigurable.showSettingsDialog()
            HTML_DESCRIPTION_SUPPORT -> SupportDialog.show()
            HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION -> TranslateService.translator.checkConfiguration(true)
            else -> return false
        }

        return true
    }

}