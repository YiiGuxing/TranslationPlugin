package cn.yiiguxing.plugin.translate.ui.notification.banner

import com.intellij.openapi.util.NlsContexts
import javax.swing.Icon

@DslMarker
private annotation class EditorBannerDsl

@EditorBannerDsl
class EditorBannerBuilder {
    var status: EditorBanner.Status = EditorBanner.Status.INFO

    private var _message: String? = null
    var message: String
        get() = checkNotNull(_message?.takeIf { it.isNotBlank() }) { "Message must not be null or blank" }
        set(@NlsContexts.Label value) {
            require(value.isNotBlank()) { "Message must not be blank" }
            _message = value
        }

    var showCloseButton: Boolean = true

    private val _actions = mutableListOf<EditorBanner.Action>()
    val actions: List<EditorBanner.Action> get() = _actions

    fun action(@NlsContexts.Label text: String, icon: Icon? = null, runnable: Runnable): EditorBanner.Action {
        require(text.isNotBlank()) { "Action text must not be blank" }
        return EditorBanner.Action(
            text = text,
            icon = icon,
            runnable = runnable
        ).also {
            _actions += it
        }
    }

    fun build(): EditorBanner = EditorBanner(
        status = status,
        message = message,
        showCloseButton = showCloseButton,
        actions = actions
    )
}

fun editorBanner(block: EditorBannerBuilder.() -> Unit): EditorBanner = EditorBannerBuilder().apply(block).build()
