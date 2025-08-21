package cn.yiiguxing.plugin.translate.documentation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications

internal object DocumentationNotifications {

    @JvmStatic
    val EDITOR_BANNER_INFO_KEY = Key.create<BannerInfo>("translation.documentation.notification.banner")

    data class BannerInfo(val message: String, val actionText: String, val action: Runnable)

    fun setEditorBanner(editor: Editor, bannerInfo: BannerInfo?) {
        editor.putUserData(EDITOR_BANNER_INFO_KEY, bannerInfo)
        editor.virtualFile?.let {
            EditorNotifications.getInstance(editor.project ?: return).updateNotifications(it)
        }
    }

    fun getEditorBannerInfo(editor: Editor): BannerInfo? {
        return editor.getUserData(EDITOR_BANNER_INFO_KEY)
    }
}