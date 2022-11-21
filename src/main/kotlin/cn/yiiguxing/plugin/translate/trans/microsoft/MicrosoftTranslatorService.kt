package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.Http.userAgent
import cn.yiiguxing.plugin.translate.util.UrlBuilder
import cn.yiiguxing.plugin.translate.util.getCommonMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException


/**
 * Service for the Microsoft Translator API.
 */
@Service
internal class MicrosoftTranslatorService {

    private var accessToken: String? = null

    private var expireAt: Long = -1

    private var tokenPromise: Promise<String>? = null

    @Synchronized
    private fun updateAccessToken(token: String) {
        accessToken = token
        expireAt = System.currentTimeMillis() + EXPIRATION
        tokenPromise = null
    }

    @Synchronized
    private fun getValidAccessToken(): String? {
        val token = accessToken
        return if (token != null && System.currentTimeMillis() < expireAt) {
            token
        } else {
            null
        }
    }

    @Synchronized
    private fun getTokenPromise(): Promise<String> {
        synchronized(this) {
            tokenPromise?.let {
                return@getTokenPromise it
            }
        }

        val promise = runAsync { Http.get(AUTH_URL) { userAgent() } }
            .onSuccess(::updateAccessToken)

        synchronized(this) {
            tokenPromise = promise
        }

        return promise
    }

    /**
     * Returns the access token. If the token has expired, it will be refreshed.
     */
    @RequiresBackgroundThread
    fun getAccessToken(): String {
        getValidAccessToken()?.let { token ->
            return token
        }

        val promise = getTokenPromise()
        val token = try {
            promise.blockingGet(TIMEOUT)
        } catch (e: TimeoutException) {
            throw AuthenticationException("Authentication failed: timeout", e)
        } catch (e: Throwable) {
            clearTokenPromise(promise)

            val ex = if (e is ExecutionException) e.cause ?: e else e
            val message = if (ex is IOException) ex.getCommonMessage() else ex.message
            throw AuthenticationException(
                "Authentication failed${if (message.isNullOrEmpty()) "" else ": $message"}",
                ex
            )
        }

        if (token == null) {
            clearTokenPromise(promise)
            throw AuthenticationException("Authentication failed: cannot get access token")
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

        // 12-minutes expiration time. Actually, the real expiration time is 15 minutes.
        // Here we use 3 minutes as the error retention time.
        private const val EXPIRATION = 12 * 60 * 1000

        private const val TIMEOUT = 10 * 1000 // 10 seconds

        private const val AUTH_URL = "https://edge.microsoft.com/translate/auth"

        private const val TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate"

        /**
         * Returns the [MicrosoftTranslatorService] instance.
         */
        val service: MicrosoftTranslatorService get() = service()

        @RequiresBackgroundThread
        fun translate(text: String, from: Lang, to: Lang, textType: TextType = TextType.PLAIN): String {
            val translateUrl = UrlBuilder(TRANSLATE_URL)
                .addQueryParameter("api-version", "3.0")
                .apply { if (from != Lang.AUTO) addQueryParameter("from", from.microsoftLanguageCode) }
                .addQueryParameter("to", to.microsoftLanguageCode)
                .addQueryParameter("textType", textType.value)
                .build()

            return Http.postJson(translateUrl, listOf(MicrosoftTranslationSource(text))) { auth() }
        }

        private fun RequestBuilder.auth() {
            val accessToken = service.getAccessToken()
            tuner { it.setRequestProperty("Authorization", "Bearer $accessToken") }
        }

    }

    class AuthenticationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

}