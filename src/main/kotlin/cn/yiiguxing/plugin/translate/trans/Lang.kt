package cn.yiiguxing.plugin.translate.trans

/**
 * 语言
 */
enum class Lang(val langName: String, val code: String) {
    AUTO("自动检测", "auto"),
    CHINESE("中文", "zh-CHS"),
    ENGLISH("英文", "en"),
    JAPANESE("日文", "ja"),
    KOREAN("韩文", "ko"),
    FRENCH("法文", "fr"),
    ARABIC("阿拉伯文", "ar"),
    POLISH("波兰文", "pl"),
    DANISH("丹麦文", "da"),
    GERMAN("德文", "de"),
    RUSSIAN("俄文", "ru"),
    FINNISH("芬兰文", "fi"),
    DUTCH("荷兰文", "nl"),
    CZECH("捷克文", "cs"),
    ROMANIAN("罗马尼亚文", "ro"),
    NORWEGIAN("挪威文", "no"),
    PORTUGUESE("葡萄牙文", "pt"),
    SWEDISH("瑞典文", "sv"),
    SLOVAK("斯洛伐克文", "sk"),
    SPANISH("西班牙文", "es"),
    HINDI("印地文", "hi"),
    INDONESIAN("印度尼西亚文", "id"),
    ITALIAN("意大利文", "it"),
    THAI("泰文", "th"),
    TURKISH("土耳其文", "tr"),
    GREEK("希腊文", "el"),
    HUNGARY("匈牙利文", "hu")
}
