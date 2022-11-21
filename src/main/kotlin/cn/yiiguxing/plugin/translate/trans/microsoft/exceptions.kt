package cn.yiiguxing.plugin.translate.trans.microsoft

import java.io.IOException

class MicrosoftAuthenticationException(
    message: String? = null,
    cause: Throwable? = null
) : IOException(message, cause)

class MicrosoftStatusCodeException(
    val message: String?,
    val error: MicrosoftErrorMessage?,
    val statusCode: Int
) {
    constructor(message: String?, statusCode: Int) : this(message, null, statusCode)
}