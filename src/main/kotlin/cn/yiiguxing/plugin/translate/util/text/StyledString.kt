package cn.yiiguxing.plugin.translate.util.text

data class StyledString(
    val string: String,
    val style: String,
    val date: Any? = null
) : CharSequence by string {
    override fun toString() = string
}