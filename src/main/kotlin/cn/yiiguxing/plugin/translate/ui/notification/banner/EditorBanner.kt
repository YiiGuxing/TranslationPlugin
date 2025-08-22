package cn.yiiguxing.plugin.translate.ui.notification.banner

import com.intellij.openapi.util.NlsContexts
import javax.swing.Icon

data class EditorBanner(
    val status: Status,
    @NlsContexts.Label
    val message: String,
    val showCloseButton: Boolean = true,
    val actions: List<Action> = emptyList()
) {

    enum class Status { INFO, SUCCESS, WARNING, ERROR, PROMO }

    data class Action(
        @NlsContexts.LinkLabel
        val text: String,
        val icon: Icon? = null,
        val runnable: Runnable
    )
}
