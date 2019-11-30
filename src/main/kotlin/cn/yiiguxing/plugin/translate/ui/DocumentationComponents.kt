package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.util.w
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import java.lang.reflect.Method

private val LOGGER: Logger = Logger.getInstance("cn.yiiguxing.plugin.translate.ui.DocumentationComponents")

private val SET_TEXT_METHOD: Method? by lazy {
    try {
        DocumentationComponent::class.java.getMethod(
            "setText",
            String::class.java,
            PsiElement::class.java,
            Boolean::class.java
        )
    } catch (e: Throwable) {
        LOGGER.w("Cant not get method: setText(String, PsiElement, Boolean)", e)
        null
    }
}

private val SET_DATA_METHOD: Method? by lazy {
    try {
        DocumentationComponent::class.java.getMethod(
            "setData",
            PsiElement::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            DocumentationProvider::class.java
        )
    } catch (e: Throwable) {
        LOGGER.w("Cant not get method: setData(PsiElement, String, String, String, DocumentationProvider)", e)
        null
    }
}

fun DocumentationComponent.setContent(content: String, element: PsiElement?) {
    when {
        SET_TEXT_METHOD != null -> SET_TEXT_METHOD?.invoke(this, content, element, true)
        SET_DATA_METHOD != null -> SET_DATA_METHOD?.invoke(this, element, content, null, null, null)
    }
}