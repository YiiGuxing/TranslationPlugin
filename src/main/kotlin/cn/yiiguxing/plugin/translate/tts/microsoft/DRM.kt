package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.sha256

private const val WIN_EPOCH = 11644473600L
private const val S_TO_NS = 1000000000L

internal object DRM {

    /**
     * Returns the Sec-MS-GEC version.
     */
    fun getSecMsGecVersion(): String {
        return "1-${Http.getAgentChromiumVersion()}"
    }

    /**
     * Generates the Sec-MS-GEC token.
     */
    fun generateSecMsGecToken(): String {
        // Get the current timestamp in seconds
        var ticks = System.currentTimeMillis() / 1000
        // Convert to Windows file time epoch (1601-01-01 00:00:00 UTC)
        ticks += WIN_EPOCH
        // Round down to the nearest 5 minutes (300 seconds)
        ticks -= ticks % 300
        // Convert to 100-nanosecond intervals (Windows file time format)
        ticks *= S_TO_NS / 100

        return "$ticks$TRUSTED_CLIENT_TOKEN".sha256().uppercase()
    }
}