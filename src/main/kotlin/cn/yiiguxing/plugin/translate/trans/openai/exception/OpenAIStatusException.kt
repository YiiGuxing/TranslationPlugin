package cn.yiiguxing.plugin.translate.trans.openai.exception

import com.intellij.util.io.HttpRequests

class OpenAIStatusException(
    message: String,
    statusCode: Int,
    url: String,
    val error: OpenAiError? = null
) : HttpRequests.HttpStatusException(message, statusCode, url)