@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util.credential

/**
 * String credential manager interface.
 */
interface StringCredentialManager {

    /** The credential. */
    var credential: String?

    /** Returns `true` if the credential is set. */
    val isCredentialSet: Boolean

    /**
     * Removes the credential.
     */
    fun removeCredential() {
        credential = null
    }

}