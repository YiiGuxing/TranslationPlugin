package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.HelpTopic
import com.intellij.openapi.help.WebHelpProvider

class HelpProvider : WebHelpProvider() {

    override fun getHelpPageUrl(helpTopicId: String): String {
        return HelpTopic.of(helpTopicId).url
    }

}