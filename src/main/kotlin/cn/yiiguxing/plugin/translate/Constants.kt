/**
 * Constants
 */
@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate

import java.nio.file.Path
import java.nio.file.Paths

private val USER_HOME_PATH = System.getProperty("user.home")
val TRANSLATION_DIRECTORY: Path = Paths.get(USER_HOME_PATH, ".translation")

const val DEFAULT_NOTIFICATION_GROUP_ID = "Translation Plugin"
const val UPDATE_NOTIFICATION_GROUP_ID = "Translation Plugin Updates"

const val STORAGE_NAME = "yiiguxing.translation.xml"

const val GITHUB_URL = "https://github.com/YiiGuxing/TranslationPlugin"
const val NEW_ISSUES_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new/choose"
const val OPEN_COLLECTIVE_URL = "https://opencollective.com/translation-plugin"
const val SUPPORT_PATRONS_URL = "http://yiiguxing.github.io/TranslationPlugin/support.html#patrons"
const val SUPPORT_SHARE_URL = "https://plugins.jetbrains.com/plugin/8579-translation"

const val HTML_DESCRIPTION_SETTINGS = "#SETTINGS"
const val HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION = "#TRANSLATOR_CONFIGURATION"
const val HTML_DESCRIPTION_SUPPORT = "#SUPPORT"

const val GOOGLE_TRANSLATE_HOST = "translate.googleapis.com"
const val GOOGLE_TRANSLATE_HOST_CN = "translate.google.cn"
const val GOOGLE_TRANSLATE_URL_FORMAT = "https://%s/translate_a/single"
const val GOOGLE_DOCUMENTATION_TRANSLATE_URL_FORMAT = "https://%s/translate_a/t"
const val GOOGLE_TTS_FORMAT = "https://%s/translate_tts"

const val YOUDAO_TRANSLATE_URL = "https://openapi.youdao.com/api"
const val YOUDAO_AI_URL = "http://ai.youdao.com"

const val BAIDU_TRANSLATE_URL = "http://api.fanyi.baidu.com/api/trans/vip/translate"
const val BAIDU_FANYI_URL = "http://api.fanyi.baidu.com/api/trans/product/desktop?req=developer"