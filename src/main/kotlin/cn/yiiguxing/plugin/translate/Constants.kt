/**
 * Constants
 */
@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val USER_HOME_PATH = System.getProperty("user.home")
private val DEFAULT_TRANSLATION_DIRECTORY = Paths.get(USER_HOME_PATH, ".translation")

val TRANSLATION_DIRECTORY: Path =
    if (SystemInfo.isLinux && !Files.exists(DEFAULT_TRANSLATION_DIRECTORY)) {
        System.getenv("XDG_DATA_HOME")
            ?.takeIf { it.isNotEmpty() }
            ?.let { Paths.get(it, ".translation") }
            ?: DEFAULT_TRANSLATION_DIRECTORY
    } else {
        DEFAULT_TRANSLATION_DIRECTORY
    }


const val STORAGE_NAME = "yiiguxing.translation.xml"

const val GITHUB_URL = "https://github.com/YiiGuxing/TranslationPlugin"
const val NEW_ISSUES_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new/choose"
const val OPEN_COLLECTIVE_URL = "https://opencollective.com/translation-plugin"
const val SUPPORT_SHARE_URL = "https://plugins.jetbrains.com/plugin/8579-translation"

const val HTML_DESCRIPTION_SETTINGS = "#SETTINGS"
const val HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION = "#TRANSLATOR_CONFIGURATION"
const val HTML_DESCRIPTION_SUPPORT = "#SUPPORT"