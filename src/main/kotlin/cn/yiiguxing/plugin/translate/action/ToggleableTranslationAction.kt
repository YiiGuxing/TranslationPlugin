@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsActions
import icons.TranslationIcons
import java.util.function.Supplier


abstract class ToggleableTranslationAction : UpdateInBackgroundCompatAction {

    constructor() : super()

    @JvmOverloads
    constructor(
        text: @NlsActions.ActionText String,
        description: @NlsActions.ActionDescription String? = null,
    ) : this(
        text.toPresentationText(),
        description.toPresentationText(),
    )

    @JvmOverloads
    constructor(
        dynamicText: Supplier<@NlsActions.ActionText String?>,
        dynamicDescription: Supplier<@NlsActions.ActionDescription String?> = Presentation.NULL_STRING,
    ) : super(dynamicText, dynamicDescription, null)

    protected abstract fun isSelected(event: AnActionEvent): Boolean

    protected abstract fun setSelected(event: AnActionEvent, state: Boolean)

    final override fun update(event: AnActionEvent) {
        val state = isSelected(event)
        with(event.presentation) {
            isTranslationSelected = state
            icon = if (state) TranslationIcons.Translation else TranslationIcons.TranslationInactivated
        }
        update(event, state)
    }

    protected open fun update(event: AnActionEvent, isSelected: Boolean) {
    }

    final override fun actionPerformed(event: AnActionEvent) {
        val state = !isSelected(event)
        setSelected(event, state)
        event.presentation.isTranslationSelected = state
    }

    companion object {
        private val TRANSLATION_SELECTED_KEY: Key<Boolean> = Key.create("translation-selected")

        internal var Presentation.isTranslationSelected: Boolean
            set(value) = putClientProperty(TRANSLATION_SELECTED_KEY, value)
            get() = getClientProperty(TRANSLATION_SELECTED_KEY) == true

        private fun String?.toPresentationText(): Supplier<String?> = when (this) {
            null -> Presentation.NULL_STRING
            else -> Supplier { this }
        }
    }
}
