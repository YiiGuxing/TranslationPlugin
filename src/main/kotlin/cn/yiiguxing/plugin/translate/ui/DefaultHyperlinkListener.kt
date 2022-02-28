package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.util.Hyperlinks
import com.intellij.ui.BrowserHyperlinkListener
import javax.swing.event.HyperlinkEvent

open class DefaultHyperlinkListener : BrowserHyperlinkListener() {

    override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
        if (!Hyperlinks.handleDefaultHyperlinkActivated(hyperlinkEvent)) {
            super.hyperlinkActivated(hyperlinkEvent)
        }
    }

    companion object : DefaultHyperlinkListener()
}