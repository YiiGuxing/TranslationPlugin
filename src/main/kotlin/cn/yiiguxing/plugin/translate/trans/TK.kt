/*
 * 计算谷歌翻译的tk值.
 * 
 * Created by Yii.Guxing on 2017/10/27
 */
package cn.yiiguxing.plugin.translate.trans

import java.lang.Math.abs
import java.util.*

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

    private val generator = Random()

    var values = update()
        private set

    fun update(): Pair<Long, Long> {
        val a = abs(generator.nextInt().toLong())
        val b = generator.nextInt().toLong()
        val c = System.currentTimeMillis() / MIM

        values = c to (a + b)
        return values
    }

    @Suppress("unused")
    fun updateFromGoogle(): Pair<Long, Long> =
            // TODO get from https://translate.google.cn/translate_a/element.js
            // TODO 懒得写了。。。
            update()
}

/**
 * 计算tk值.
 */
fun String.tk(tkk: Pair<Long, Long> = TKK.values): String {
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

    val d = tkk.first
    val e = tkk.second
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