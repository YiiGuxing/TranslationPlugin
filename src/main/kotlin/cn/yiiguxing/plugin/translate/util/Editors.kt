package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange

fun Editor.createCaretRangeMarker(selectionRange: TextRange): RangeMarker {
    return document
        .createRangeMarker(selectionRange)
        .apply {
            isGreedyToLeft = true
            isGreedyToRight = true
        }
}