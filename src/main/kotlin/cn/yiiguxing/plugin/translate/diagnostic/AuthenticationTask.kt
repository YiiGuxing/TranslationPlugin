package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.w
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import kotlin.math.max


internal class AuthenticationTask(
    project: Project?,
    component: JComponent,
    private val verification: GitHubVerification
) :
    Task.WithResult<GitHubCredentials, Exception>(
        project, component, message("error.account.authentication.title"), true
    ) {

    override fun compute(indicator: ProgressIndicator): GitHubCredentials {
        indicator.text2 = message("error.account.authentication.message")

        while (System.currentTimeMillis() < verification.expiresTimestamp) {
            indicator.checkCanceled()
            val response = try {
                authorize()
            } catch (e: IOException) {
                LOG.w("Authentication failed.", e)
                null
            }

            indicator.checkCanceled()
            response?.let {
                if (response.isSuccessful) {
                    val token = response.getToken()
                    val user = fetchGitHubUser(token)
                    indicator.checkCanceled()
                    return GitHubCredentials(user, token)
                }

                LOG.d("Github authentication error response: $response")

                if (response.errorCode !in CAN_CONTINUE_ERROR_CODES) {
                    throw GitHubAuthenticationException(response.errorCode!!, response.errorDescription ?: "")
                }
            }

            val interval = max(verification.interval, response?.newInterval ?: 0)
            waitForGitHub(indicator, interval * 1000L)
        }

        indicator.checkCanceled()
        throw GitHubAuthenticationException(
            "operation_timed_out",
            message("error.account.authentication.timeout.message")
        )
    }

    fun queueAndGet(): GitHubCredentials? {
        return try {
            ProgressManager.getInstance().run(this)
        } catch (e: ProcessCanceledException) {
            null
        }
    }

    private fun authorize(): AuthenticationResponse {
        return Http.post<AuthenticationResponse>(
            OAUTH_URL,
            "client_id" to GITHUB_CLIENT_ID,
            "device_code" to verification.deviceCode,
            "grant_type" to GRANT_TYPE
        )
    }

    private fun fetchGitHubUser(token: GitHubToken): GitHubUser {
        return try {
            Http.request("https://api.github.com/user") {
                accept("application/vnd.github.v3+json")
                tuner { it.setRequestProperty("Authorization", token.authorizationToken) }
            }
        } catch (e: IOException) {
            GitHubUser(-1, "Unknown")
        }
    }

    private fun waitForGitHub(indicator: ProgressIndicator, intervalMillis: Long) {
        val service = AppExecutorUtil.getAppScheduledExecutorService()
        val future = service.schedule(
            { LOG.debug("Ready to poll GitHub device status") },
            intervalMillis,
            TimeUnit.MILLISECONDS
        )
        ProgressIndicatorUtils.awaitWithCheckCanceled(future, indicator)
    }


    private data class AuthenticationResponse(
        @SerializedName("error")
        val errorCode: String?,
        @SerializedName("error_description")
        val errorDescription: String?,
        @SerializedName("interval")
        val newInterval: Int?,
        @SerializedName("access_token")
        val accessToken: String?,
        @SerializedName("token_type")
        val tokenType: String?,
        @SerializedName("scope")
        val scope: String?
    ) {
        val isSuccessful: Boolean get() = errorCode == null

        fun getToken(): GitHubToken {
            check(isSuccessful) {
                "Authentication failed, unable to get token. errorCode=$errorCode, errorDescription=$errorDescription"
            }

            return GitHubToken(accessToken!!, tokenType!!, scope!!)
        }

        override fun toString(): String {
            val sb = StringBuilder("AuthenticationResponse(")
            if (isSuccessful) {
                sb.append("accessToken=**********, ")
                sb.append("tokenType=", tokenType, ", ")
                sb.append("scope=", scope)
            } else {
                sb.append("errorCode=", errorCode, ", ")
                sb.append("errorDescription=", errorDescription, ", ")
                sb.append("newInterval=", newInterval)
            }
            sb.append(")")
            return sb.toString()
        }
    }


    companion object {
        private const val OAUTH_URL = "https://github.com/login/oauth/access_token"
        private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"

        private val CAN_CONTINUE_ERROR_CODES: Array<out String?> = arrayOf("authorization_pending", "slow_down")

        private val LOG = Logger.getInstance(AuthenticationTask::class.java)
    }
}