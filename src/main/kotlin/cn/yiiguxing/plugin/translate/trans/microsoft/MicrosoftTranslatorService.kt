package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftSourceText
import cn.yiiguxing.plugin.translate.trans.microsoft.data.TextType
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.Http.userAgent
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * Service for the Microsoft Translator API.
 */
@Service
internal class MicrosoftTranslatorService {

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
                    userAgent()
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
            throw if (ex is IOException) {
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

        private const val TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate"

        private val GSON: Gson = Gson()
        private val LOG: Logger = logger<MicrosoftTranslatorService>()

        /**
         * Returns the [MicrosoftTranslatorService] instance.
         */
        private val service: MicrosoftTranslatorService get() = service()

        @RequiresBackgroundThread
        fun translate(text: String, from: Lang, to: Lang, textType: TextType = TextType.PLAIN): String {
            val translateUrl = UrlBuilder(TRANSLATE_URL)
                .addQueryParameter("api-version", "3.0")
                .apply { if (from != Lang.AUTO) addQueryParameter("from", from.microsoftLanguageCode) }
                .addQueryParameter("to", to.microsoftLanguageCode)
                .addQueryParameter("textType", textType.value)
                .build()

            return MicrosoftHttp.post(translateUrl, listOf(MicrosoftSourceText(text))) { auth() }
        }

        private fun RequestBuilder.auth() {
            val accessToken = service.getAccessToken()
            tuner { it.setRequestProperty("Authorization", "Bearer $accessToken") }
        }

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
