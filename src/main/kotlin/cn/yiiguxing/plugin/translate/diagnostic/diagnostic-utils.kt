package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.util.toHexString
import com.intellij.util.io.DigestUtil
import java.security.MessageDigest
import java.util.*

private const val CAUSE_CAPTION = "CAUSE"
private const val SUPPRESSED_CAPTION = "SUPPRESSED"

/**
 * Returns the MD5 digest of the [Throwable], used to identify its uniqueness.
 *
 * Note: The generated MD5 digest is based on [throwable][Throwable]'s
 * [stack trace][Throwable.stackTrace] and does not include its [message][Throwable.message],
 * So it can eliminate the influence of the dynamic content that may exist in the
 * [message][Throwable.message] on its uniqueness. Also, since the [message][Throwable.message]
 * is not used as input to generate the MD5 digest, it is also inaccurate,
 * but it is very useful in error reporting, it can avoid a lot of duplicate
 * error reports being submitted. So relative to the benefit it brings,
 * this loss of accuracy is acceptable.
 */
internal fun Throwable.md5(): String {
    val md5 = DigestUtil.md5()
    // Guard against malicious overrides of Throwable.equals by
    // using a Set with identity equality semantics.
    val dejaVu = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())

    updateMD5(md5, emptyArray(), null, dejaVu)

    return md5.digest().toHexString()
}

private fun Throwable.updateMD5(
    md5: MessageDigest,
    enclosingTrace: Array<out StackTraceElement>,
    caption: String?,
    dejaVu: MutableSet<Throwable>
) {
    if (this in dejaVu) {
        md5.update("[CIRCULAR REFERENCE: ${javaClass.name}]".toByteArray(Charsets.UTF_8))
        return
    }

    dejaVu.add(this)
    caption?.let { md5.update(it.toByteArray(Charsets.UTF_8)) }
    md5.update(javaClass.name.toByteArray(Charsets.UTF_8))

    val trace = stackTrace
    var m = trace.size - 1
    var n = enclosingTrace.size - 1
    while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
        m--
        n--
    }
    for (i in 0..m) {
        md5.update(trace[i].toString().toByteArray(Charsets.UTF_8))
    }

    val framesInCommon = trace.size - 1 - m
    if (framesInCommon != 0) {
        md5.update(framesInCommon.toString().toByteArray(Charsets.UTF_8))
    }

    for (se in suppressed) {
        se.updateMD5(md5, trace, SUPPRESSED_CAPTION, dejaVu)
    }
    cause?.updateMD5(md5, trace, CAUSE_CAPTION, dejaVu)
}