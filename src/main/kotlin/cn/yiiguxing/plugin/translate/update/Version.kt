package cn.yiiguxing.plugin.translate.update


data class Version constructor(val version: String = "v0.0") {

    val versionNumbers: Pair<Int, Int> by lazy { version.toVersionParts() }

    val versionString: String by lazy { "${versionNumbers.first}.${versionNumbers.second}" }

    override fun toString(): String = "Version(version=$version, versionNumbers=$versionNumbers)"

    operator fun compareTo(other: Version): Int {
        val compare = versionNumbers.first.compareTo(other.versionNumbers.first)
        return if (compare == 0) versionNumbers.second.compareTo(other.versionNumbers.second) else compare
    }

    companion object {
        private fun String.toVersionParts(): Pair<Int, Int> {
            val versionString = if (this[0].equals('v', true)) substring(1) else this
            val versionParts = versionString.split('.', '-').take(2)
            return when (versionParts.size) {
                1 -> versionParts[0].toInt() to 0
                2 -> versionParts[0].toInt() to versionParts[1].toInt()
                else -> throw IllegalStateException("Invalid version number: $this")
            }
        }
    }

}