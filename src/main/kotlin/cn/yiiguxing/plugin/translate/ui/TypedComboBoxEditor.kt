package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.ui.FixedComboBoxEditor

class TypedComboBoxEditor<T>(private val transform: (String) -> T?) : FixedComboBoxEditor() {

    override fun getItem(): T? = field.text
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            val value = transform(it)
            val stringValue = value.toString()
            if (stringValue != it) {
                field.text = stringValue
            }
            value
        }

}