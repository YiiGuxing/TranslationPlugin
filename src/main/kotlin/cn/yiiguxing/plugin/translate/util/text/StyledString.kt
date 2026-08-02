package cn.yiiguxing.plugin.translate.util.text

import java.util.stream.IntStream

data class StyledString(
    val string: String,
    val style: String,
    val date: Any? = null
) : CharSequence by string {
    override fun toString() = string
    override fun chars(): IntStream = string.chars()
    override fun codePoints(): IntStream = string.codePoints()
}