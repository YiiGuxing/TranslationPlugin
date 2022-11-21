package cn.yiiguxing.plugin.translate.trans.edge

import cn.yiiguxing.plugin.translate.trans.google.userAgent
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.d
import com.google.gson.Gson
import java.util.*
import com.intellij.openapi.diagnostic.Logger


object Auth {

    private val logger: Logger = Logger.getInstance(Auth::class.java)
    private val gson = Gson()
    private val decoder = Base64.getUrlDecoder()
    private const val TRANSLATE_AUTH_URL = "https://edge.microsoft.com/translate/auth"

    private var innerToken: String? = null
    private var jwtPayload: JwtPayload? = null

    internal val token: String
        @Synchronized
        get() {
            if (innerToken.isNullOrBlank() || jwtPayload == null) updateToken()
            jwtPayload?.let { jwtPayload ->
                val surplus = jwtPayload.exp - System.currentTimeMillis() / 1000
                if (surplus < 60) {
                    logger.d("Jwt ready to expire：${surplus}")
                    updateToken()
                }
            }
            return innerToken ?: ""
        }

    private fun updateToken() {
        innerToken = Http.get(TRANSLATE_AUTH_URL) {
            userAgent()
        }
        jwtPayload = decodeJwtPayload(innerToken!!)
    }

    private fun decodeJwtPayload(jwt: String): JwtPayload? {
        val payloadBase64 = jwt.split('.').getOrNull(1) ?: return null
        return gson.fromJson(decode(payloadBase64), JwtPayload::class.java).also { logger.d("Edge jwt: $it") }
    }

    private fun decode(base64Url: String) = String(decoder.decode(base64Url))
}

