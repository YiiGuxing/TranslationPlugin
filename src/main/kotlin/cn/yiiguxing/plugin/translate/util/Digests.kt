package cn.yiiguxing.plugin.translate.util

import com.intellij.util.io.DigestUtil
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun String.getMessageDigest(messageDigest: MessageDigest): String {
    return with(messageDigest) {
        update(toByteArray(Charsets.UTF_8))
        digest().toHexString()
    }
}

/**
 * 生成32位MD5摘要
 * @return MD5摘要
 */
fun String.md5(): String = getMessageDigest(DigestUtil.md5())

/**
 * 生成SHA-256摘要
 * @return SHA-256摘要
 */
fun String.sha256(): String = getMessageDigest(DigestUtil.sha256())

/**
 * 生成Base64格式的MD5摘要
 */
fun String.md5Base64(): String {
    return with(DigestUtil.md5()) {
        update(toByteArray())
        Base64.getEncoder().encodeToString(digest())
    }
}

/**
 * 生成Base64格式的`HMACSha1`摘要
 */
fun String.hmacSha1(key: String): String {
    val mac: Mac = Mac.getInstance("HMACSha1")
    val secretKeySpec = SecretKeySpec(key.toByteArray(), mac.algorithm)
    mac.init(secretKeySpec)
    return Base64.getEncoder().encodeToString(mac.doFinal(toByteArray()))
}

/**
 * 生成SHA-1摘要
 */
fun Path.sha1(): String {
    val digest = DigestUtil.sha1()
    DigestUtil.updateContentHash(digest, this)
    return DigestUtil.digestToHash(digest)
}