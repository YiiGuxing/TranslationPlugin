/*
 * 计算谷歌翻译的tk值.
 */
package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.util.Http.userAgent
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.lang.StrictMath.abs
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern


/**
 * 计算谷歌翻译的tkk值.
 */
object TKK {
    private const val MIM = 60 * 60 * 1000
    private const val ELEMENT_URL_PATH = "/translate_a/element.js"

    private val log: Logger = logger<TKK>()

    private val tkkPattern = Pattern.compile("tkk='(\\d+).(-?\\d+)'")

    private var innerValue: Pair<Long, Long>? = null


    val value: Pair<Long, Long>
        @RequiresBackgroundThread
        get() = update() ?: generate()


    @RequiresBackgroundThread
    fun update(): Pair<Long, Long>? {
        synchronized(this) {
            innerValue?.let { tkk ->
                val now = System.currentTimeMillis() / MIM
                if (tkk.first == now) {
                    return tkk
                }
            }
        }

        val newTKK = updateFromGoogle()

        synchronized(this) {
            val oldTKK = innerValue
            if (oldTKK == null || (newTKK != null && newTKK.first >= oldTKK.first)) {
                innerValue = newTKK
            }
            return innerValue
        }
    }

    /**
     * 本地生成一个TKK值，该值对普通翻译有效，而对文档翻译是无效。
     */
    private fun generate(): Pair<Long, Long> {
        val now = System.currentTimeMillis() / MIM
        val generator = ThreadLocalRandom.current()
        return now to (abs(generator.nextInt().toLong()) + generator.nextInt().toLong())
    }

    private fun getElementJsRequest(serverUrl: String): RequestBuilder =
        HttpRequests.request("${serverUrl.trimEnd('/')}$ELEMENT_URL_PATH")
            .userAgent()
            .googleReferer()
            .connectTimeout(5000)
            .throwStatusCodeException(true)

    private fun updateFromGoogle(): Pair<Long, Long>? {
        return try {
            fetchTKK(googleApiServerUrl).also { (v1, v2) ->
                log.i("TKK Updated: $v1.$v2")
            }
        } catch (error: Throwable) {
            log.w("TKK update failed", error)
            null
        }
    }

    /**
     * 从谷歌翻译的网页中获取TKK值。
     * @throws IllegalStateException 当TKK值无法从网页中获取时。
     */
    internal fun fetchTKK(serverUrl: String = googleApiServerUrl): Pair<Long, Long> {
        val elementJS = getElementJsRequest(serverUrl).readString(null)
        val matcher = tkkPattern.matcher(elementJS)

        if (!matcher.find()) {
            throw IllegalStateException("TKK not found.")
        }

        val value1 = matcher.group(1).toLong()
        val value2 = matcher.group(2).toLong()

        return value1 to value2
    }

    internal fun testConnection(): Boolean = try {
        getElementJsRequest(googleApiServerUrl).tryConnect()
        true
    } catch (e: Throwable) {
        false
    }.also {
        log.i("TKK connection test: ${if (it) "OK" else "FAILURE"}")
    }
}

/**
 * 计算tk值.
 */
@RequiresBackgroundThread
fun String.tk(tkk: Pair<Long, Long> = TKK.value): String {
    val a = mutableListOf<Long>()
    var b = 0
    while (b < length) {
        var c = this[b].code
        if (128 > c) {
            a += c.toLong()
        } else {
            if (2048 > c) {
                a += (c shr 6 or 192).toLong()
            } else {
                if (55296 == (c and 64512) && b + 1 < length && 56320 == (this[b + 1].code and 64512)) {
                    c = 65536 + ((c and 1023) shl 10) + (this[++b].code and 1023)
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
        f = calculate(f, "+-a^+6")
    }
    f = calculate(f, "+-3^+b+-f")
    f = f xor e
    if (0 > f) {
        f = (f and Int.MAX_VALUE.toLong()) + Int.MAX_VALUE.toLong() + 1
    }
    f = (f % 1E6).toLong()

    return "$f.${f xor d}"
}

private fun calculate(a: Long, b: String): Long {
    var g = a
    for (c in 0..b.length - 2 step 3) {
        val d = b[c + 2]
        val e = if ('a' <= d) d.code - 87 else d.toString().toInt()
        val f = if ('+' == b[c + 1]) g.ushr(e) else g shl e
        g = if ('+' == b[c]) g + f and (Int.MAX_VALUE.toLong() * 2 + 1) else g xor f
    }

    return g
}