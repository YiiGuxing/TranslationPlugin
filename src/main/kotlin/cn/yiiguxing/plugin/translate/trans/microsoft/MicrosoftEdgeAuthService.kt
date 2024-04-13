package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import cn.yiiguxing.plugin.translate.util.w
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Service for the Microsoft Edge authentication.
 */
@Service
internal class MicrosoftEdgeAuthService private constructor() {

    private var accessToken: String? = null

    private var expireAt: Long = -1

    private var tokenPromise: Promise<String>? = null

    private val validAccessToken: String?
        @Synchronized
        get() = accessToken?.takeIf { System.currentTimeMillis() < expireAt }

    private fun updateAccessToken(token: String) {
        val expirationTime = getExpirationTimeFromToken(token)
        LOG.d("Update access token: ********, Expiration time: ${Date(expirationTime)}")
        synchronized(this) {
            accessToken = token
            expireAt = expirationTime - PRE_EXPIRATION
            tokenPromise = null
        }
    }

    @Synchronized
    private fun getTokenPromise(): Promise<String> {
        tokenPromise?.let {
            return it
        }

        val promise = asyncLatch { latch ->
            runAsync {
                latch.await(100, TimeUnit.MILLISECONDS)
                Http.get(AUTH_URL) {
                    accept("*/*")
                    setUserAgent()
                }.also {
                    if (!it.matches(JWT_REGEX)) {
                        throw MicrosoftAuthenticationException("Authentication failed: Invalid token.")
                    }
                }
            }
                .onError { LOG.w("Failed to get access token", it) }
                .onSuccess(::updateAccessToken)
        }

        tokenPromise = promise

        return promise
    }

    /**
     * Returns the access token. If the token has expired, it will be refreshed.
     */
    @RequiresBackgroundThread
    fun getAccessToken(): String {
        validAccessToken?.let { token ->
            return token
        }

        val promise = getTokenPromise()
        val token = try {
            promise.blockingGet(TIMEOUT)
        } catch (e: TimeoutException) {
            LOG.warn("Authentication failed: timeout", e)
            throw MicrosoftAuthenticationException("Authentication failed: timeout", e)
        } catch (e: Throwable) {
            clearTokenPromise(promise)

            LOG.warn("Authentication failed", e)
            val ex = if (e is ExecutionException) e.cause ?: e else e
            throw if (ex !is MicrosoftAuthenticationException && ex is IOException) {
                MicrosoftAuthenticationException("Authentication failed: ${ex.getCommonMessage()}", ex)
            } else ex
        }

        if (token == null) {
            clearTokenPromise(promise)
            LOG.warn("Authentication failed: cannot get access token")
            throw MicrosoftAuthenticationException("Authentication failed: cannot get access token")
        }

        return token
    }

    @Synchronized
    private fun clearTokenPromise(whosePromise: Promise<*>) {
        if (whosePromise == tokenPromise) {
            tokenPromise = null
        }
    }

    companion object {
        private const val TIMEOUT = 60 * 1000 // 1 minute
        private const val PRE_EXPIRATION = 2 * 60 * 1000 // 2 minutes
        private const val DEFAULT_EXPIRATION = 10 * 60 * 1000 // 10 minutes

        private const val AUTH_URL = "https://edge.microsoft.com/translate/auth"
        private val JWT_REGEX = Regex("""^[a-zA-Z0-9\-_]+(\.[a-zA-Z0-9\-_]+){2}$""")

        private val GSON: Gson = Gson()
        private val LOG: Logger = logger<MicrosoftEdgeAuthService>()

        /**
         * Returns the [MicrosoftEdgeAuthService] instance.
         */
        fun service(): MicrosoftEdgeAuthService = service<MicrosoftEdgeAuthService>()

        private data class JwtPayload(@SerializedName("exp") val expirationTime: Long)

        private fun getExpirationTimeFromToken(token: String): Long {
            return try {
                val payloadChunk = token.split('.')[1]
                val decoder = Base64.getUrlDecoder()
                val payload = String(decoder.decode(payloadChunk))
                GSON.fromJson(payload, JwtPayload::class.java).expirationTime * 1000
            } catch (e: Throwable) {
                System.currentTimeMillis() + DEFAULT_EXPIRATION - PRE_EXPIRATION / 2
            }
        }
    }
}