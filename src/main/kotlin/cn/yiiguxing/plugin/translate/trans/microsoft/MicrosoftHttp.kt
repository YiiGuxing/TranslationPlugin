package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftErrorMessage
import cn.yiiguxing.plugin.translate.trans.microsoft.data.presentableError
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

internal object MicrosoftHttp {

    private const val JSON_MIME_TYPE = "application/json"

    private val GSON: Gson = Gson()
    private val LOG: Logger = logger<MicrosoftHttp>()

    fun post(url: String, data: Any, init: RequestBuilder.() -> Unit): String {
        return HttpRequests.post(url, JSON_MIME_TYPE)
            .accept(JSON_MIME_TYPE)
            .apply(init)
            .throwStatusCodeException(false)
            .connect {
                it.write(GSON.toJson(data))
                checkResponseCode(it.connection as HttpURLConnection)
                it.readString()
            }
    }

    private fun checkResponseCode(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        if (responseCode < 400) {
            return
        }

        val statusLine = "$responseCode ${connection.responseMessage}"
        val errorText = getErrorText(connection)
        LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Error $statusLine body:\n$errorText")

        val jsonError = errorText?.let { getJsonError(connection, it) }
        jsonError ?: LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

        if (jsonError != null) {
            MicrosoftStatusCodeException(
                "$statusLine - ${jsonError.presentableError}",
                jsonError,
                responseCode
            )
        } else {
            MicrosoftStatusCodeException("$statusLine - $errorText", responseCode)
        }
    }

    private fun getErrorText(connection: HttpURLConnection): String? {
        val errorStream = connection.errorStream ?: return null
        val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
        return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun getJsonError(connection: HttpURLConnection, errorText: String): MicrosoftErrorMessage? {
        if (!connection.contentType.startsWith(JSON_MIME_TYPE)) {
            return null
        }
        return try {
            return GSON.fromJson(errorText, MicrosoftErrorMessage::class.java)
        } catch (jse: JsonParseException) {
            null
        }
    }
}