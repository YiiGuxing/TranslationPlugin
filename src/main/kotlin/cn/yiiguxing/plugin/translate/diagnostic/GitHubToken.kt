package cn.yiiguxing.plugin.translate.diagnostic

internal data class GitHubToken(
    val accessToken: String,
    val tokenType: String,
    val scope: String
) {

    val authorizationToken: String get() = "$tokenType $accessToken"

    override fun toString(): String {
        return "GitHubToken(accessToken='**********', tokenType='$tokenType', scope='$scope')"
    }
}
