package cn.yiiguxing.plugin.translate.util

import java.text.DecimalFormat


object ByteSize {

    private const val KB_SHIFT = 10
    private const val MB_SHIFT = 20
    private val FORMATTER = DecimalFormat("#.#")

    /**
     * Format to #.#MB, #.#KB, or #B.
     */
    fun format(bytes: Long): String {
        require(bytes >= 0)
        return when {
            bytes == 0L -> "0KB"
            bytes shr MB_SHIFT > 0 -> "${FORMATTER.format(bytes.toDouble() / (1 shl MB_SHIFT))}MB"
            bytes shr KB_SHIFT > 0 -> "${FORMATTER.format(bytes.toDouble() / (1 shl KB_SHIFT))}KB"
            else -> "${bytes}B"
        }
    }
}
