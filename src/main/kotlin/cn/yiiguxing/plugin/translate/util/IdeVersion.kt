package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber

// http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
enum class IdeVersion(val buildNumber: Int) {
    IDE2018_1(181),
    IDE2019_1(191),
    IDE2019_3(193);

    companion object {

        val buildNumber: BuildNumber get() = ApplicationInfo.getInstance().build

        @Suppress("MemberVisibilityCanBePrivate")
        val BUILD_NUMBER = buildNumber.baselineVersion

        val isIde2018OrNewer: Boolean = BUILD_NUMBER >= IDE2018_1.buildNumber

        val isIde2019OrNewer: Boolean = BUILD_NUMBER >= IDE2019_1.buildNumber

        val isIde2019_3OrNewer: Boolean = BUILD_NUMBER >= IDE2019_3.buildNumber
    }
}

