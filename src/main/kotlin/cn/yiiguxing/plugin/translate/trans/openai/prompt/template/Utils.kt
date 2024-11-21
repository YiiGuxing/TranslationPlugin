package cn.yiiguxing.plugin.translate.trans.openai.prompt.template

import com.intellij.openapi.util.text.Strings

/**
 * Utility functions for prompt template.
 *
 * DO NOT use this class directly in code.
 */
@Suppress("unused")
@Deprecated("DO NOT use this class directly in code.")
internal object Utils {

    /**
     * Tests if the specified [text] is multiline.
     */
    fun isMultiline(text: String): Boolean = text.isNotEmpty() && text.contains('\n')

    /**
     * Detects a common minimal indent of all the input lines,
     * removes it from every line and also removes the first
     * and the last lines if they are blank.
     *
     * Note that blank lines do not affect the detected indent level.
     *
     * In case if there are non-blank lines with no leading whitespace
     * characters (no indent at all) then the common indent is `0`,
     * and therefore this function doesn't change the indentation.
     *
     * Doesn't preserve the original line endings.
     */
    fun trimIndent(text: String) = text.trimIndent()

    /**
     * Returns a string having leading whitespace removed.
     */
    fun trimStart(text: String): String = text.trimStart()

    /**
     * Returns a string having trailing whitespace removed.
     */
    fun trimEnd(text: String): String = text.trimEnd()

    /**
     * Returns a string having leading and trailing characters from the [chars] string removed.
     */
    fun trim(text: String, chars: String): String = text.trim(*chars.toCharArray())

    /**
     * Returns a string having leading characters from the [chars] string removed.
     */
    fun trimStart(text: String, chars: String): String = text.trimStart(*chars.toCharArray())

    /**
     * Returns a string having trailing characters from the [chars] string removed.
     */
    fun trimEnd(text: String, chars: String): String = text.trimEnd(*chars.toCharArray())

    /**
     * Returns a string having leading [prefix] removed.
     */
    @JvmOverloads
    fun trimPrefix(text: String, prefix: String, ignoreCase: Boolean = false): String {
        return if (text.startsWith(prefix, ignoreCase)) {
            text.substring(prefix.length)
        } else {
            text
        }
    }

    /**
     * Returns a string having trailing [suffix] removed.
     */
    @JvmOverloads
    fun trimSuffix(text: String, suffix: String, ignoreCase: Boolean = false): String {
        return if (text.endsWith(suffix, ignoreCase)) {
            text.substring(0, text.length - suffix.length)
        } else {
            text
        }
    }

    /**
     * Indent the specified [text].
     *
     * @param text the text to indent
     * @param indent the number of spaces to indent, must be greater than or equal to 0
     * @return the indented text
     */
    fun indent(text: String, indent: Int): String {
        require(indent >= 0) { "indent must be greater than or equal to 0" }

        if (text.isEmpty()) {
            return ""
        }
        if (indent == 0) {
            return text
        }

        val spaces = " ".repeat(indent)
        return text.lineSequence().joinToString("\n") { line -> "$spaces$line" }
    }

    /**
     * Returns [text] with some characters replaced with
     * standard XML entities, e.g. `<` replaced with `&lt;`.
     */
    fun escapeXmlEntities(text: String): String = Strings.escapeXmlEntities(text)

    /**
     * Returns [text] with standard XML entities replaced with
     * their corresponding characters, e.g. `&lt;` replaced with `<`.
     */
    fun unescapeXmlEntities(text: String): String = Strings.unescapeXmlEntities(text)
}