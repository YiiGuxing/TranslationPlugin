package cn.yiiguxing.plugin.translate.trans.microsoft.models

import kotlin.time.Duration


internal sealed interface AzureAuthentication {

    data class AccessToken(val value: String) : AzureAuthentication

    data class SubscriptionKey(val key: String, val region: String? = null) : AzureAuthentication

}

internal data class BingAuthentication(
    val ig: String,
    val iid: String,
    val key: String,
    val token: String,
    val ttl: Duration
)