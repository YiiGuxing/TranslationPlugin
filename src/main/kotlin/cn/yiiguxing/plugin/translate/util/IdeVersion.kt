@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber

// http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
enum class IdeVersion(val buildNumber: Int) {
    IDE2020_3(203),
    IDE2022_1(221);

    companion object {
        val buildNumber: BuildNumber get() = ApplicationInfo.getInstance().build

        operator fun compareTo(ideVersion: IdeVersion): Int {
            return buildNumber.baselineVersion.compareTo(ideVersion.buildNumber)
        }
    }
}