/*
 * 单词选择工具
 * 
 * Created by Yii.Guxing on 2017/9/11
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange


typealias CharCondition = SelectWordUtil.CharCondition

/**
 * 默认条件
 */
@Suppress("PropertyName")
val DEFAULT_CONDITION: CharCondition = SelectWordUtil.JAVA_IDENTIFIER_PART_CONDITION
/**
 * 非英文条件
 */
@Suppress("PropertyName")
val NON_LATIN_CONDITION: CharCondition = CharCondition { it.toInt() > 0xFF && Character.isJavaIdentifierPart(it) }

private val textRangeComparator = Comparator<TextRange> { tr1, tr2 ->
    if (tr2.contains(tr1)) -1 else 1
}

/**
 * 取词模式
 */
enum class SelectionMode {
    /**
     * 取最近的单个词
     */
    EXCLUSIVE,
    /**
     * 以最大范围取最近的所有词
     */
    INCLUSIVE
}

/**
 * Returns the first character matching the given [condition], or `null` if no such character was found.
 */
fun String.find(condition: CharCondition): Char? = find { condition.value(it) }

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
        isExclusive -> ranges[0]
        else -> ranges.maxWith(textRangeComparator)
    }
}
