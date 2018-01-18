@file:Suppress("unused")

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
 * TranslationUIManager
 * <p>
 * Created by Yii.Guxing on 2017-09-14 0014.
 */
class TranslationUIManager private constructor() {

    // TODO 为不同的Project分配不同的Balloon和Dialog，Project之间不共用Balloon或Dialog

    var translationBalloon: TranslationBalloon? = null
        private set
    var translationDialog: TranslationDialog? = null
        private set


    /**
     * 显示气泡
     *
     * @param editor           编辑器
     * @param caretRangeMarker 光标范围
     * @param queryText        查询字符串
     * @return 气泡实例
     */
    fun showBalloon(editor: Editor, caretRangeMarker: RangeMarker, queryText: String): TranslationBalloon {
        translationBalloon?.hide()

        return TranslationBalloon(editor, caretRangeMarker, queryText).apply {
            translationBalloon = this
            Disposer.register(this, Disposable { translationBalloon = null })
            show()
        }
    }

    /**
     * 显示对话框
     *
     * @return 对话框实例
     */
    fun showDialog(project: Project?): TranslationDialog {
        val dialog = translationDialog ?: TranslationDialog(project).apply {
            translationDialog = this
            Disposer.register(this, Disposable { translationDialog = null })
        }

        return dialog.apply { show() }
    }

    /**
     * 关闭显示中的气泡和对话框
     */
    fun closeAll() {
        translationBalloon?.hide()
        translationDialog?.close()
    }

    companion object {
        val instance: TranslationUIManager
            get() = ServiceManager.getService(TranslationUIManager::class.java)
    }
}