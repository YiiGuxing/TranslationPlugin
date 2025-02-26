@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

enum class AzureServiceVersion(val value: String) {
    V2024_10_21("2024-10-21"),
    V2024_06_01("2024-06-01"),
    V2024_02_01("2024-02-01"),
    V2023_05_15("2023-05-15"),
    V2025_01_01_PREVIEW("2025-01-01-preview");

    companion object {
        fun previewVersions() = AzureServiceVersion.values().filter {
            it.value.endsWith("preview", ignoreCase = true)
        }
    }
}