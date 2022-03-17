@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package cn.yiiguxing.plugin.translate.diagnostic

class GitHubAuthenticationException(
    val errorCode: String,
    val errorDescription: String
) : Exception("[$errorCode]$errorDescription")