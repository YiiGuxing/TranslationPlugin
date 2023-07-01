package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.diagnostic.Logger
import java.io.OutputStream
import java.math.BigInteger
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.SecureRandom

private val random by lazy { SecureRandom() }

private val LOG = Logger.getInstance("#cn.yiiguxing.plugin.translate.util.IO")

// http://stackoverflow.com/a/41156 - shorter than UUID, but secure
private fun randomToken(): String {
    return BigInteger(130, random).toString(32)
}

fun Path.writeSafe(outConsumer: (OutputStream) -> Unit): Path {
    val tempFile = parent.resolve("${fileName}.${randomToken()}.tmp")
    Files.newOutputStream(tempFile).use(outConsumer)
    try {
        Files.move(tempFile, this, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } catch (e: AtomicMoveNotSupportedException) {
        LOG.w(e)
        Files.move(tempFile, this, StandardCopyOption.REPLACE_EXISTING)
    }
    return this
}