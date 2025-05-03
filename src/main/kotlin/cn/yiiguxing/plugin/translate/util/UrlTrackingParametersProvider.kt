package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.TranslationPlugin
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.logger
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import java.net.URISyntaxException

object UrlTrackingParametersProvider {

    fun augmentIdeUrl(originalUrl: String, vararg extra: Pair<String, String>): String {
        try {
            return URIBuilder(originalUrl)
                .setParameters(extra.map { (name, value) -> BasicNameValuePair(name, value) })
                .setParameter("utm_source", "intellij")
                .setParameter("utm_medium", "plugin")
                .setParameter("utm_campaign", IdeVersion.buildNumber.productCode)
                .setParameter("utm_content", ApplicationInfo.getInstance().shortVersion)
                .build()
                .toString()
        } catch (e: URISyntaxException) {
            logger<UrlTrackingParametersProvider>().warn(originalUrl, e)
            return originalUrl
        }
    }

    fun augmentUrl(originalUrl: String, vararg extra: Pair<String, String>): String {
        try {
            return URIBuilder(originalUrl)
                .setParameters(extra.map { (name, value) -> BasicNameValuePair(name, value) })
                .setParameter("utm_source", "intellij-plugin")
                .setParameter("utm_medium", "link")
                .setParameter("utm_campaign", TranslationPlugin.adName)
                .setParameter("utm_content", TranslationPlugin.version)
                .build()
                .toString()
        } catch (e: URISyntaxException) {
            logger<UrlTrackingParametersProvider>().warn(originalUrl, e)
            return originalUrl
        }
    }
}