package cn.yiiguxing.plugin.translate.diagnostic.github.auth

internal data class GitHubDeviceToken(
    val accessToken: String,
    val tokenType: String,
    val scope: String
) {

    val authorizationToken: String get() = "$tokenType $accessToken"

    override fun toString(): String {
        return "GitHubDeviceToken(accessToken='**********', tokenType='$tokenType', scope='$scope')"
    }
}
