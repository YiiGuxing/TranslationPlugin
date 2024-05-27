package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.w
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


internal class GitHubDeviceAuthTask(
    project: Project?,
    component: JComponent?,
    private val clientId: String,
    private val deviceCode: GitHubDeviceCode
) :
    Task.WithResult<GitHubCredentials, Exception>(
        project, component, message("github.waiting.authentication.task.title"), true
    ) {

    companion object {
        private val LOG = Logger.getInstance(GitHubDeviceAuthTask::class.java)
        private val CAN_CONTINUE_ERROR_CODES: Array<out String?> = arrayOf("authorization_pending", "slow_down")
    }

    override fun compute(indicator: ProgressIndicator): GitHubCredentials {
        indicator.text2 = message("github.waiting.authentication.task.message", deviceCode.userCode)

        while (System.currentTimeMillis() < deviceCode.expiresTimestamp) {
            indicator.checkCanceled()
            val response = try {
                GitHubDeviceAuthApis.auth(clientId, deviceCode.code)
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
                    throw GitHubDeviceAuthException(response.errorCode!!, response.errorDescription ?: "")
                }
            }

            val interval = max(deviceCode.interval, response?.newInterval ?: 0)
            waitForGitHub(indicator, interval * 1000L)
        }

        indicator.checkCanceled()
        throw GitHubDeviceAuthException(
            "operation_timed_out",
            message("github.authentication.timeout.message")
        )
    }

    fun queueAndGet(): GitHubCredentials? {
        return try {
            ProgressManager.getInstance().run(this)
        } catch (e: ProcessCanceledException) {
            null
        }
    }

    private fun fetchGitHubUser(token: GitHubDeviceToken): GitHubUser {
        return try {
            GitHubDeviceAuthApis.fetchUser(token)
        } catch (e: Throwable) {
            // The GitHub user is not important, so the acquisition failure does not affect the following things.
            if (e is IOException) {
                LOG.w("Failed to fetch GitHub user.", e)
            } else {
                // We still need to solve non-IO exceptions, so we need to display prompts.
                LOG.e("Failed to fetch GitHub user.", e)
            }
            GitHubUser.UNKNOWN_USER
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
}