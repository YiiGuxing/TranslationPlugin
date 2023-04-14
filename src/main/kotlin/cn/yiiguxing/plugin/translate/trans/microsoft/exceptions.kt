@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftErrorMessage
import com.intellij.util.io.HttpRequests
import java.io.IOException

class MicrosoftAuthenticationException(
    message: String? = null,
    cause: Throwable? = null
) : IOException(message, cause)

class MicrosoftStatusException(
    message: String,
    statusCode: Int,
    url: String,
    val error: MicrosoftErrorMessage? = null
) : HttpRequests.HttpStatusException(message, statusCode, url)