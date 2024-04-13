package cn.yiiguxing.plugin.translate.trans.microsoft.models

enum class TextType(val value: String) {
    PLAIN("plain"),
    HTML("html");

    override fun toString(): String = value
}