package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.openapi.data.AsyncExpiringData
import cn.yiiguxing.plugin.translate.openapi.data.CachePolicy
import cn.yiiguxing.plugin.translate.trans.microsoft.models.BingAuthentication
import cn.yiiguxing.plugin.translate.util.Http.setUserAgent
import com.intellij.openapi.components.Service
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.APP)
internal class BingEdgeTranslatorService private constructor(
    private val scope: CoroutineScope,
) {

    companion object {
        private const val BING_ORIGIN = "https://www.bing.com"
        private const val BING_TRANSLATOR_URL = "$BING_ORIGIN/translator"
        private const val BING_TRANSLATOR_API_URL = "$BING_ORIGIN/ttranslatev3"

        // params_AbusePreventionHelper = [<key>,"<token>",<ttl>]
        private val ABUSE_PREVENTION_REGEX =
            """params_AbusePreventionHelper\s*=\s*\[\s*(\d+)\s*,\s*"([^"]+)"\s*,\s*(\d+)\s*]""".toRegex()
        private val IG_REGEX = """IG\s*:\s*"([A-Fa-f0-9]+)"""".toRegex()
        private val IID_REGEX = """data-iid\\s*=\\s*"([^"]+)"""".toRegex()

        private const val EXPIRY_SAFETY_MARGIN_MS = 60_000L // 1 minute
    }

    private val authService: AsyncExpiringData<BingAuthentication> =
        object : AsyncExpiringData<BingAuthentication>(scope) {
            override suspend fun load(): BingAuthentication {
                val html = HttpRequests.request(BING_TRANSLATOR_URL).setUserAgent().readString()
                return parseBingAuthentication(html)
            }

            override fun cachePolicy(value: BingAuthentication): CachePolicy {
                return CachePolicy.ExpireAfter(value.ttl)
            }
        }


    private fun parseBingAuthentication(html: String): BingAuthentication {
        val matchResult = ABUSE_PREVENTION_REGEX.find(html)
        requireNotNull(matchResult) { "Failed to parse AbusePreventionHelper from Bing Translator page" }
        val ig = IG_REGEX.find(html)?.groupValues?.getOrNull(1)
        requireNotNull(ig) { "Failed to parse IG from Bing Translator page" }
        val iid = IID_REGEX.find(html)?.groupValues?.getOrNull(1)
        requireNotNull(iid) { "Failed to parse IID from Bing Translator page" }

        val (key, token, ttlMs) = matchResult.destructured
        val ttl = ttlMs.toLong().milliseconds - EXPIRY_SAFETY_MARGIN_MS.milliseconds
        return BingAuthentication(
            ig = ig,
            iid = iid,
            key = key,
            token = token,
            ttl = maxOf(ttl, 0.milliseconds)
        )
    }
}