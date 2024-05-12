package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.util.sha256
import com.intellij.openapi.components.service
import java.util.*

object YoudaoRequestHelper {

    fun getSignedParams(q: String): Map<String, String> {
        val credentialSettings = service<Settings>().youdaoTranslateSettings
        val appId = credentialSettings.appId
        val privateKey = credentialSettings.getAppKey()
        val salt = UUID.randomUUID().toString()
        val curTime = (System.currentTimeMillis() / 1000).toString()
        val qInSign = if (q.length <= 20) q else "${q.take(10)}${q.length}${q.takeLast(10)}"
        val sign = "$appId$qInSign$salt$curTime$privateKey".sha256()

        @Suppress("SpellCheckingInspection")
        return mapOf(
            "q" to q,
            "appKey" to appId,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curTime,
        )
    }

}