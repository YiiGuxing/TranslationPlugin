package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.userAgent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.concurrency.runAsync


/**
 * Service for the Microsoft Translator API.
 */
@Service
internal class MicrosoftTranslatorService {

    private var accessToken: String? = null

    private var expireAt: Long = -1

    private var promise: Promise<String>? = null

    @Synchronized
    private fun updateAccessToken(token: String) {
        accessToken = token
        expireAt = System.currentTimeMillis() + EXPIRATION
        promise = null
    }

    @Synchronized
    private fun getAccessTokenPromise(): Promise<String> {
        val token = accessToken
        if (token != null && System.currentTimeMillis() < expireAt) {
            return resolvedPromise(token)
        }

        promise?.let {
            return@getAccessTokenPromise it
        }

        return runAsync { Http.get(AUTH_URL) { userAgent() } }
            .onSuccess(::updateAccessToken)
            .also { promise = it }
    }

    /**
     * Returns the access token. If the token has expired, it will be refreshed.
     */
    @RequiresBackgroundThread
    fun getAccessToken(): String {
        return getAccessTokenPromise()
            .blockingGet(TIMEOUT)
            ?: throw AuthenticationException("Authentication failed")
    }


    companion object {

        // 12-minutes expiration time. Actually, the real expiration time is 15 minutes.
        // Here we use 3 minutes as the error retention time.
        private const val EXPIRATION = 12 * 60 * 1000

        private const val TIMEOUT = 10 * 1000 // 10 seconds

        private const val AUTH_URL = "https://edge.microsoft.com/translate/auth"

        /**
         * Returns the [MicrosoftTranslatorService] instance.
         */
        private val service: MicrosoftTranslatorService get() = service()

    }

    class AuthenticationException(message: String) : RuntimeException(message)

}