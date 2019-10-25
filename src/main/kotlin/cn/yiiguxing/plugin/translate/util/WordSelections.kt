/*
 * 单词选择工具
 */
@file:Suppress("unused", "NOTHING_TO_INLINE")

package cn.yiiguxing.plugin.translate.util

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange


typealias CharCondition = SelectWordUtil.CharCondition

inline operator fun CharCondition.invoke(char: Char): Boolean = value(char)

/**
 * Java标识符条件
 */
val JAVA_IDENTIFIER_PART_CONDITION: CharCondition = SelectWordUtil.JAVA_IDENTIFIER_PART_CONDITION
/**
 * 默认条件
 */
val DEFAULT_CONDITION: CharCondition = JAVA_IDENTIFIER_PART_CONDITION
/**
 * 非英文条件
 */
val NON_LATIN_CONDITION: CharCondition = CharCondition { it.toInt() > 0xFF && Character.isJavaIdentifierPart(it) }
/**
 * 非空白条件
 */
val NON_WHITESPACE_CONDITION: CharCondition = CharCondition { !Character.isWhitespace(it) }

private val TextRangeComparator = Comparator<TextRange> { tr1, tr2 ->
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
fun String.find(condition: CharCondition): Char? = find { condition(it) }

/**
 * Returns `true` if at least one character matches the given [condition].
 */
fun String.any(condition: CharCondition): Boolean = any { condition(it) }

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
        else -> ranges.maxWith(TextRangeComparator)
    }
}
