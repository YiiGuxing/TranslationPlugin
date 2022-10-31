/*
 * 计算谷歌翻译的tk值.
 */
package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.Logger
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
    private const val ELEMENT_URL = "https://translate.googleapis.com/translate_a/element.js"

    private val logger: Logger = Logger.getInstance(GoogleTranslator::class.java)

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

    private fun getElementJsRequest(): RequestBuilder = HttpRequests.request(ELEMENT_URL)
        .userAgent()
        .googleReferer()
        .connectTimeout(5000)
        .throwStatusCodeException(true)

    private fun updateFromGoogle(): Pair<Long, Long>? {
        return try {
            val elementJS = getElementJsRequest().readString(null)
            val matcher = tkkPattern.matcher(elementJS)
            if (matcher.find()) {
                val value1 = matcher.group(1).toLong()
                val value2 = matcher.group(2).toLong()

                logger.i("TKK Updated: $value1.$value2")

                value1 to value2
            } else {
                logger.w("TKK update failed: TKK not found.")
                null
            }
        } catch (error: Throwable) {
            logger.w("TKK update failed", error)
            null
        }
    }

    internal fun testConnection(): Boolean = try {
        getElementJsRequest().tryConnect()
        true
    } catch (e: Throwable) {
        false
    }.also {
        logger.i("TKK connection test: ${if (it) "OK" else "FAILURE"}")
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