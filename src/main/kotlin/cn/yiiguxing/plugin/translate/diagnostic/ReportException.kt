package cn.yiiguxing.plugin.translate.diagnostic

import com.intellij.openapi.diagnostic.Attachment

class ReportException(
    message: String,
    vararg val attachments: Attachment,
    cause: Throwable? = null
) : RuntimeException(message, cause)