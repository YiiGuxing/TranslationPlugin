package cn.yiiguxing.plugin.translate.util

object CamelCaseSplitter {

    fun split(text: String): List<String> {
        if (!(text as CharSequence).any { !it.isLowerCase() || !it.isLetter() }) {
            return listOf(text)
        }

        val result = ArrayList<String>()
        var index = 0
        var wordStartIndex = -1
        var prevType = Character.MATH_SYMBOL.toInt()

        while (index < text.length) {
            val char = text[index]
            val curType: Int = Character.getType(char)
            if (isLetter(curType)) {
                if (!isLetter(prevType) && wordStartIndex >= 0) {
                    result.add(text.substring(wordStartIndex, index))
                    wordStartIndex = -1
                }

                if (wordStartIndex < 0) {
                    wordStartIndex = index
                } else if (
                    curType == Character.UPPERCASE_LETTER.toInt() &&
                    prevType == Character.LOWERCASE_LETTER.toInt()
                ) {
                    result.add(text.substring(wordStartIndex, index))
                    wordStartIndex = index
                } else if (
                    index - wordStartIndex > 1 &&
                    curType == Character.LOWERCASE_LETTER.toInt() &&
                    prevType == Character.UPPERCASE_LETTER.toInt()
                ) {
                    result.add(text.substring(wordStartIndex, index - 1))
                    wordStartIndex = index - 1
                }
            } else if (wordStartIndex < 0 || !isLetter(prevType) && prevType == curType) {
                if (wordStartIndex < 0) {
                    wordStartIndex = index
                }
            } else {
                result.add(text.substring(wordStartIndex, index))
                wordStartIndex = index
            }

            prevType = curType
            index++
        }

        if (wordStartIndex >= 0) {
            result.add(text.substring(wordStartIndex, index))
        }

        return result.toList()
    }

    private fun isLetter(type: Int): Boolean {
        return when (type) {
            Character.UPPERCASE_LETTER.toInt(),
            Character.LOWERCASE_LETTER.toInt(),
            Character.TITLECASE_LETTER.toInt(),
            Character.MODIFIER_LETTER.toInt(),
            Character.OTHER_LETTER.toInt() -> true

            else -> false
        }
    }
}