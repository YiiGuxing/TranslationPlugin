package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.util.toHexString
import com.intellij.util.io.DigestUtil
import java.security.MessageDigest
import java.util.*

private const val CAUSE_CAPTION = "CAUSE"
private const val SUPPRESSED_CAPTION = "SUPPRESSED"

private const val PLUGIN_PACKAGE = "cn.yiiguxing.plugin.translate"

/**
 * Generate an ID for the [Throwable], used to identify its uniqueness.
 *
 * Note: The generated ID is based on [throwable][Throwable]'s
 * [stack trace][Throwable.stackTrace] and does not include its [message][Throwable.message],
 * So it can eliminate the influence of the dynamic content that may exist in the
 * [message][Throwable.message] on its uniqueness. Also, since the [message][Throwable.message]
 * is not used as input to generate the ID, it is also inaccurate,
 * but it is very useful in error reporting, it can avoid a lot of duplicate
 * error reports being submitted. So relative to the benefit it brings,
 * this loss of accuracy is acceptable.
 */
internal fun Throwable.generateId(): String {
    val md5 = DigestUtil.md5()
    // Guard against malicious overrides of Throwable.equals by
    // using a Set with identity equality semantics.
    val dejaVu = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())

    update(md5, emptyArray(), null, dejaVu)

    return md5.digest().toHexString()
}

private fun Throwable.update(
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
    val lastIndexWithoutCommonFrames = trace.lastIndexWithoutCommonFrames(enclosingTrace)
    val firstEndEdgeOfPluginFrame = trace.firstEndEdgeOfPluginFrame()

    var traceEndIndex = trace.lastIndex
    if (firstEndEdgeOfPluginFrame >= 0) {
        traceEndIndex = firstEndEdgeOfPluginFrame
    }
    if (lastIndexWithoutCommonFrames in 0 until traceEndIndex) {
        traceEndIndex = lastIndexWithoutCommonFrames
    }

    for (i in 0..traceEndIndex) {
        md5.update(trace[i].toString().toByteArray(Charsets.UTF_8))
    }

    for (se in suppressed) {
        se.update(md5, trace, SUPPRESSED_CAPTION, dejaVu)
    }
    cause?.update(md5, trace, CAUSE_CAPTION, dejaVu)
}

private fun Array<out StackTraceElement>.firstEndEdgeOfPluginFrame(): Int {
    var index = -1
    for (i in indices) {
        if (this[i].className.startsWith(PLUGIN_PACKAGE)) {
            index = i
        } else if (index >= 0) {
            break
        }
    }

    return index
}

private fun Array<out StackTraceElement>.lastIndexWithoutCommonFrames(other: Array<out StackTraceElement>): Int {
    var m = lastIndex
    var n = other.lastIndex
    while (m >= 0 && n >= 0 && this[m] == other[n]) {
        m--
        n--
    }
    return m
}