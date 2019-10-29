/*
 * 计算谷歌翻译的tk值.
 */
package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.lang.StrictMath.abs
import java.util.*
import java.util.regex.Pattern

private fun `fun`(a: Long, b: String): Long {
    var g = a
    for (c in 0..b.length - 2 step 3) {
        val d = b[c + 2]
        val e = if ('a' <= d) d.toInt() - 87 else d.toString().toInt()
        val f = if ('+' == b[c + 1]) g.ushr(e) else g shl e
        g = if ('+' == b[c]) g + f and (Int.MAX_VALUE.toLong() * 2 + 1) else g xor f
    }

    return g
}

/**
 * 计算谷歌翻译的tkk值.
 */
object TKK {
    private const val MIM = 60 * 60 * 1000
    private const val ELEMENT_URL_FORMAT = "https://%s/translate_a/element.js"
    private const val NOTIFICATION_DISPLAY_ID = "TKK Update Failed"

    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

    private val generator = Random()

    private val tkkPattern = Pattern.compile("tkk='(\\d+).(-?\\d+)'")

    private var innerValue: Pair<Long, Long> = 0L to 0L

    private var needUpdate: Boolean = true

    val value get() = update()

    @Synchronized
    fun update(): Pair<Long, Long> {
        val now = System.currentTimeMillis() / MIM
        val (curVal) = innerValue
        if (!needUpdate && now == curVal) {
            return innerValue
        }

        val newTKK = updateFromGoogle()
        needUpdate = newTKK == null
        innerValue = newTKK ?: now to (abs(generator.nextInt().toLong()) + generator.nextInt().toLong())
        return innerValue
    }

    private fun updateFromGoogle(): Pair<Long, Long>? {
        val updateUrl = ELEMENT_URL_FORMAT.format(googleHost)

        return try {
            val elementJS = HttpRequests.request(updateUrl)
                .userAgent()
                .googleReferer()
                .readString(null)
            val matcher = tkkPattern.matcher(elementJS)
            if (matcher.find()) {
                val value1 = matcher.group(1).toLong()
                val value2 = matcher.group(2).toLong()

                logger.i("TKK Updated: $value1.$value2")

                value1 to value2
            } else {
                logger.w("TKK update failed: TKK not found.")
                Notifications.showWarningNotification(
                    NOTIFICATION_DISPLAY_ID,
                    "TKK",
                    "TKK update failed: TKK not found."
                )

                null
            }
        } catch (e: Throwable) {
            val error = NetworkException.wrapIfIsNetworkException(e, googleHost)

            logger.w("TKK update failed", error)
            Notifications.showErrorNotification(
                null,
                NOTIFICATION_DISPLAY_ID,
                "TKK",
                message("notification.ttk.update.failed"),
                error
            )

            null
        }
    }
}

/**
 * 计算tk值.
 */
fun String.tk(tkk: Pair<Long, Long> = TKK.value): String {
    val a = mutableListOf<Long>()
    var b = 0
    while (b < length) {
        var c = this[b].toInt()
        if (128 > c) {
            a += c.toLong()
        } else {
            if (2048 > c) {
                a += (c shr 6 or 192).toLong()
            } else {
                if (55296 == (c and 64512) && b + 1 < length && 56320 == (this[b + 1].toInt() and 64512)) {
                    c = 65536 + ((c and 1023) shl 10) + (this[++b].toInt() and 1023)
                    a += (c shr 18 or 240).toLong()
                    a += (c shr 12 and 63 or 128).toLong()
                } else {
                    a += (c shr 12 or 224).toLong()
                }
                a += (c shr 6 and 63 or 128).toLong()
            }
            a += (c and 63 or 128).toLong()
        }

        b++
    }

    val (d, e) = tkk
    var f = d
    for (h in a) {
        f += h
        f = `fun`(f, "+-a^+6")
    }
    f = `fun`(f, "+-3^+b+-f")
    f = f xor e
    if (0 > f) {
        f = (f and Int.MAX_VALUE.toLong()) + Int.MAX_VALUE.toLong() + 1
    }
    f = (f % 1E6).toLong()

    return "$f.${f xor d}"
}