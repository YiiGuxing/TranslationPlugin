package cn.yiiguxing.plugin.translate.ui.notification.banner

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications

object EditorBannerManager {

    @JvmStatic
    private val EDITOR_BANNER_KEY = Key.create<EditorBanner>("translation.notification.banner")

    @JvmStatic
    fun setEditorBanner(editor: Editor, banner: EditorBanner?) {
        if (editor.isDisposed) {
            return
        }

        val virtualFile = editor.virtualFile ?: return
        val project = editor.project ?: return

        editor.putUserData(EDITOR_BANNER_KEY, banner)
        EditorNotifications.getInstance(project).updateNotifications(virtualFile)
    }

    @JvmStatic
    fun setEditorBanner(editor: Editor, bannerBuilder: EditorBannerBuilder.() -> Unit) {
        setEditorBanner(editor, editorBanner(bannerBuilder))
    }

    @JvmStatic
    fun getEditorBanner(editor: Editor): EditorBanner? {
        return editor.takeUnless { editor.isDisposed }?.getUserData(EDITOR_BANNER_KEY)
    }
}