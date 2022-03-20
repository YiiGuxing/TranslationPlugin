@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package cn.yiiguxing.plugin.translate.diagnostic.github.auth

internal class GitHubDeviceAuthException(
    val errorCode: String,
    val errorDescription: String
) : Exception("[$errorCode]$errorDescription")