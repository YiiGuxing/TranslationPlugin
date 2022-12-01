package cn.yiiguxing.plugin.translate.compat

import java.util.*

val Char.code: Int get() = toInt()

fun String.lowercase(locale: Locale = Locale.getDefault()): String = toLowerCase(locale)

fun String.uppercase(locale: Locale = Locale.getDefault()): String = toUpperCase(locale)