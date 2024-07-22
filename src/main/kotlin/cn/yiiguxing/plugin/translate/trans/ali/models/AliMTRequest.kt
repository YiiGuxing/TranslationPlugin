package cn.yiiguxing.plugin.translate.trans.ali.models

import java.text.SimpleDateFormat
import java.util.*

private const val ALI_MT_API_ENDPOINT = "mt.aliyuncs.com"
private const val ALI_MT_API_VERSION = "2018-10-12"
private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
    timeZone = SimpleTimeZone(0, "GMT")
}

internal data class AliMTRequest(
    val action: String,
    val contentType: String,
    val body: String,
    val uriHost: String = ALI_MT_API_ENDPOINT,
    val uriPath: String = "/",
    val version: String = ALI_MT_API_VERSION,
    val headers: TreeMap<String, String> = TreeMap(),
    val queries: TreeMap<String, String> = TreeMap()
) {
    init {
        headers["Host"] = uriHost
        headers["x-acs-action"] = action
        headers["x-acs-version"] = version
        headers["x-acs-date"] = DATE_FORMATTER.format(Date())
        headers["x-acs-signature-nonce"] = UUID.randomUUID().toString()
    }
}