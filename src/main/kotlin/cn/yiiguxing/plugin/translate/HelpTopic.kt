package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.Plugin

enum class HelpTopic(id: String, val url: String) {

    /** Default help */
    DEFAULT("default", WebPages.getStarted()),

    /** 阿里机器翻译通用版 */
    ALI("ali", "https://www.aliyun.com/product/ai/base_alimt"),

    /** 百度翻译 */
    BAIDU("baidu", "https://fanyi-api.baidu.com/"),

    /** 有道翻译 */
    YOUDAO("youdao", "https://ai.youdao.com/product-fanyi-text.s"),

    /** DeepL */
    DEEPL("deepl", "https://www.deepl.com/docs-api");

    val id: String = "${Plugin.PLUGIN_ID}.$id"

    companion object {
        fun of(helpTopicId: String): HelpTopic {
            for (helpTopic in values()) if (helpTopic.id == helpTopicId) {
                return helpTopic
            }

            return DEFAULT
        }
    }
}