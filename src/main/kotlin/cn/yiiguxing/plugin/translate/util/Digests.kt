package cn.yiiguxing.plugin.translate.util

import com.intellij.util.io.DigestUtil
import java.nio.file.Path
import java.security.MessageDigest
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
 * 生成`HmacSHA256`摘要
 * @param key 字符串密钥，将被自动转换为字节数组
 * @return HmacSHA256的16进制字符串
 */
fun String.hmacSha256(key: String): String {
    val mac: Mac = Mac.getInstance("HmacSHA256")
    val secretKeySpec = SecretKeySpec(key.toByteArray(), mac.algorithm)
    mac.init(secretKeySpec)
    return mac.doFinal(toByteArray()).toHexString()
}

/**
 * 生成`HmacSHA256`摘要
 * @param key 字节数组密钥
 * @return HmacSHA256的二进制字节数组
 */
fun String.hmacSha256(key: ByteArray): ByteArray {
    val mac: Mac = Mac.getInstance("HmacSHA256")
    val secretKeySpec = SecretKeySpec(key, mac.algorithm)
    mac.init(secretKeySpec)
    return mac.doFinal(toByteArray())
}

/**
 * 生成SHA-1摘要
 */
fun Path.sha1(): String {
    val digest = DigestUtil.sha1()
    DigestUtil.updateContentHash(digest, this)
    return DigestUtil.digestToHash(digest)
}