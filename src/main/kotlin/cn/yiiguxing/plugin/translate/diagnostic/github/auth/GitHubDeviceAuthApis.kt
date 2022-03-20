package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import cn.yiiguxing.plugin.translate.util.Http

internal object GitHubDeviceAuthApis {

    private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
    private const val OAUTH_URL = "https://github.com/login/oauth/access_token"
    private const val USER_URL = "https://api.github.com/user"

    private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"

    fun getDeviceCode(clientId: String, vararg scopes: String): GitHubDeviceCode {
        return Http.post<GitHubDeviceCode>(
            DEVICE_CODE_URL,
            "client_id" to clientId,
            "scope" to scopes.joinToString(",")
        )
    }

    fun auth(clientId: String, deviceCode: String): GitHubDeviceAuthResponse {
        return Http.post<GitHubDeviceAuthResponse>(
            OAUTH_URL,
            "client_id" to clientId,
            "device_code" to deviceCode,
            "grant_type" to GRANT_TYPE
        )
    }

    fun fetchUser(token: GitHubDeviceToken): GitHubUser {
        return Http.request(USER_URL) {
            accept("application/vnd.github.v3+json")
            tuner { it.setRequestProperty("Authorization", token.authorizationToken) }
        }
    }

}