package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.ui.TranslationBalloon
import cn.yiiguxing.plugin.translate.ui.TranslationDialog
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

/**
 * TranslationManager
 * <p>
 * Created by Yii.Guxing on 2017-09-14 0014.
 */
class TranslationManager {

    var translationBalloon: TranslationBalloon? = null
        private set
    var translationDialog: TranslationDialog? = null
        private set


    companion object {
        val instance: TranslationManager
            get() = ServiceManager.getService(TranslationManager::class.java)
    }

    /**
     * 显示气泡
     *
     * @param editor           编辑器
     * @param caretRangeMarker 光标范围
     * @param queryText        查询字符串
     * @return 气泡实例
     */
    fun showBalloon(editor: Editor, caretRangeMarker: RangeMarker, queryText: String): TranslationBalloon {
        translationBalloon?.run { hide() }

        val balloon = TranslationBalloon(editor, caretRangeMarker).apply {
            Disposer.register(disposable, Disposable { translationBalloon = null })
            showAndQuery(queryText)
        }
        translationBalloon = balloon

        return balloon
    }

    /**
     * 显示对话框
     *
     * @return 对话框实例
     */
    fun showDialog(project: Project?): TranslationDialog {
        val dialog = translationDialog ?: TranslationDialog(project).apply {
            Disposer.register(disposable, Disposable { translationDialog = null })
            translationDialog = this
        }
        dialog.show()

        return dialog
    }

}