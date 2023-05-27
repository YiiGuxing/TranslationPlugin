@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber

// http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
enum class IdeVersion(val buildNumber: Int) {
    IDE2020_3(203),
    IDE2021_2(212),
    IDE2021_3(213),
    IDE2022_1(221),
    IDE2022_2(222),
    IDE2022_3(223),
    IDE2023_2(232);

    companion object {
        val buildNumber: BuildNumber get() = ApplicationInfo.getInstance().build

        fun isEqual(version: IdeVersion): Boolean = buildNumber.baselineVersion == version.buildNumber

        operator fun compareTo(ideVersion: IdeVersion): Int {
            return buildNumber.baselineVersion.compareTo(ideVersion.buildNumber)
        }
    }
}