package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationInfo

enum class IdeVersion(val buildNumber: Int) {
    IDE2018_1(181);

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        val BUILD_NUMBER = ApplicationInfo.getInstance().build.baselineVersion

        val isIde2018OrNewer: Boolean = BUILD_NUMBER >= IDE2018_1.buildNumber
    }
}

