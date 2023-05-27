@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.TranslationPlugin
import java.util.Collections.emptyList
import kotlin.math.min

/**
 * Semantic versioning -> [semver.org](https://semver.org)
 */
class Version(val version: String = INITIAL_VERSION) : Comparable<Version> {

    /** The major version number. */
    val major: Int

    /** The minor version number. */
    val minor: Int

    /** The patch version number. */
    val patch: Int

    /** The pre-release version number. */
    val prerelease: String?

    /** The build metadata of this version. */
    val buildMetadata: String?


    init {
        val matchResult = VERSION_REGEX.matchEntire(version)
            ?: throw IllegalArgumentException("Invalid version: $version")
        val matchGroups = matchResult.groups
        major = matchGroups[MAJOR]!!.value.toInt()
        minor = matchGroups[MINOR]!!.value.toInt()
        patch = matchGroups[PATCH]!!.value.toInt()
        prerelease = matchGroups[PRERELEASE]?.value
        buildMetadata = matchGroups[BUILD_METADATA]?.value
    }


    /** True if the version is a pre-release. */
    val isRreRelease: Boolean = prerelease != null

    private val prereleaseTokens: List<Any> by lazy {
        prerelease?.split('.')
            ?.map { if (it[0] !in '0'..'9') it else (it.toIntOrNull() ?: it) }
            ?: emptyList()
    }

    /** Returns the version in "<[major]>.<[minor]>" form. */
    fun getFeatureUpdateVersion(): String = "$major.$minor"

    /** Returns the version in "<[major]>.<[minor]>.<[patch]>" form. */
    fun getStableVersion(): String = "$major.$minor.$patch"

    /** Returns the version in "<[major]>.<[minor]>.<[patch]>[-<[prerelease]>]" form. */
    fun getVersionWithoutBuildMetadata(): String = "$major.$minor.$patch${prerelease?.let { "-$it" } ?: ""}"

    /**
     * Test if this version has feature updates relative to the [other] version
     * (Compare only [major] and [minor] versions).
     */
    fun isFeatureUpdateOf(other: Version): Boolean {
        if (major > other.major) return true
        if (major == other.major && minor > other.minor) return true
        return major == other.major &&
                minor == other.minor &&
                other.patch == 0 &&
                !isRreRelease &&
                other.isRreRelease
    }

    /**
     * Tests whether this version is same as the [other] version
     * ([Build metadata][buildMetadata] is not compared).
     */
    fun isSameVersion(other: Version): Boolean {
        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false
        return prerelease == other.prerelease
    }

    /**
     * Tests whether this version is exactly equal to the [other] version,
     * including [build metadata][buildMetadata].
     */
    fun isEqual(other: Version): Boolean {
        return version == other.version
    }

    override fun compareTo(other: Version): Int {
        var result = major.compareTo(other.major)
        if (result != 0) return result
        result = minor.compareTo(other.minor)
        if (result != 0) return result
        result = patch.compareTo(other.patch)
        if (result != 0) return result

        return when {
            prerelease == other.prerelease -> 0
            prerelease == null -> 1
            other.prerelease == null -> -1
            else -> comparePrereleaseTokens(other.prereleaseTokens)
        }
    }

    private fun comparePrereleaseTokens(tokens: List<Any>): Int {
        val myTokens = prereleaseTokens

        for (i in 0 until min(myTokens.size, tokens.size)) {
            val result = compare(myTokens[i], tokens[i])
            if (result != 0) {
                return result
            }
        }

        return myTokens.size - tokens.size
    }

    private fun compare(l: Any, r: Any): Int = when {
        l is Int && r is Int -> l.compareTo(r)
        l is String && r is String -> l.compareTo(r)
        l is String -> 1
        else -> -1
    }

    override fun toString(): String = version

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (prerelease?.hashCode() ?: 0)
        result = 31 * result + (buildMetadata?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return isSameVersion(other as Version)
    }

    companion object {
        /**
         * The initial version: `0.0.0`
         */
        const val INITIAL_VERSION = "0.0.0"

        private const val MAJOR = "major"
        private const val MINOR = "minor"
        private const val PATCH = "patch"
        private const val PRERELEASE = "prerelease"
        private const val BUILD_METADATA = "buildMetadata"
        private val VERSION_REGEX =
            """^(?<$MAJOR>0|[1-9]\d*)\.(?<$MINOR>0|[1-9]\d*)\.(?<$PATCH>0|[1-9]\d*)(?:-(?<$PRERELEASE>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?<$BUILD_METADATA>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""".toRegex()

        /**
         * Returns current version.
         */
        fun current(): Version = Version(TranslationPlugin.descriptor.version)

        /**
         * Returns the version for the given [version string][version],
         * or the result of the [defaultValue] function if the given
         * [version string][version] is an invalid version string.
         */
        inline fun getOrElse(version: String, defaultValue: () -> Version): Version {
            return try {
                Version(version)
            } catch (e: Exception) {
                defaultValue()
            }
        }
    }

}