@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.scale.JBUIScale

// UIScales

val Int.scaled: Int
    get() = JBUIScale.scale(this)

val Float.scaled: Float
    get() = JBUIScale.scale(this)