/*
 * 单词选择工具
 * 
 * Created by Yii.Guxing on 2017/9/11
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.action.AutoSelectionMode
import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange


typealias CharCondition = SelectWordUtil.CharCondition
typealias SelectionMode = AutoSelectionMode

/**
 * 默认条件
 */
val DEFAULT_CONDITION: CharCondition = SelectWordUtil.JAVA_IDENTIFIER_PART_CONDITION
/**
 * 汉字条件
 */
val HANZI_CONDITION: CharCondition = CharCondition { it in '\u4E00'..'\u9FBF' }

private val textRangeComparator = Comparator<TextRange> { tr1, tr2 ->
    if (tr2.contains(tr1)) -1 else 1
}

/**
 * 返回当前光标周围文字的范围
 *
 * @param selectionMode 选择模式
 * @param isWordPartCondition 选择条件
 */
fun Editor.getSelectionFromCurrentCaret(selectionMode: SelectionMode = SelectionMode.INCLUSIVE,
                                        isWordPartCondition: CharCondition = DEFAULT_CONDITION): TextRange? {
    val ranges = mutableListOf<TextRange>()
    val isExclusive = selectionMode == SelectionMode.EXCLUSIVE

    SelectWordUtil.addWordOrLexemeSelection(isExclusive, this, caretModel.offset, ranges, isWordPartCondition)

    return when {
        ranges.isEmpty() -> null
        isExclusive      -> ranges[0]
        else             -> ranges.maxWith(textRangeComparator)
    }
}
