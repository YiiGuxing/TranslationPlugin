package cn.yiiguxing.plugin.translate.trans.microsoft.models

import kotlin.time.Duration


internal sealed interface AzureAuthentication {

    data class AccessToken(val value: String) : AzureAuthentication

    data class SubscriptionKey(val key: String, val region: String? = null) : AzureAuthentication

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"

        @Suppress("SpellCheckingInspection")
        const val HEADER_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key"

        @Suppress("SpellCheckingInspection")
        const val HEADER_SUBSCRIPTION_REGION = "Ocp-Apim-Subscription-Region"

        const val AUTHORIZATION_BEARER_PREFIX = "Bearer "
    }
}

internal fun AzureAuthentication.AccessToken.authorizationHeaderValue(): String =
    AzureAuthentication.AUTHORIZATION_BEARER_PREFIX + value

internal data class BingAuthentication(
    val ig: String,
    val iid: String,
    val key: String,
    val token: String,
    val ttl: Duration
)