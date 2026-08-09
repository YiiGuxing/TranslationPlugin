package cn.yiiguxing.plugin.translate.trans.microsoft.models


internal sealed interface AzureAuthentication {

    data class AccessToken(val value: String) : AzureAuthentication

    data class SubscriptionKey(val key: String, val region: String? = null) : AzureAuthentication

}