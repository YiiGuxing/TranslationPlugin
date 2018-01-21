@file:Suppress("unused")

package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.ui.TranslationBalloon
import cn.yiiguxing.plugin.translate.ui.TranslationDialog
import cn.yiiguxing.plugin.translate.ui.TranslatorWidget
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.PositionTracker

/**
 * TranslationUIManager
 * <p>
 * Created by Yii.Guxing on 2017-09-14 0014.
 */
class TranslationUIManager private constructor() {

    private val balloonMap: MutableMap<Project?, TranslationBalloon> = mutableMapOf()
    private val dialogMap: MutableMap<Project?, TranslationDialog> = mutableMapOf()

    /**
     * 显示气泡
     *
     * @param editor   编辑器
     * @param text     查询字符串
     * @param tracker  位置跟踪器
     * @param position 气泡位置
     * @return 气泡实例
     */
    fun showBalloon(editor: Editor, text: String, tracker: PositionTracker<Balloon>, position: Balloon.Position)
            : TranslationBalloon {
        checkThread()
        val project = editor.project
        balloonMap[project]?.hide()

        return TranslationBalloon(editor, text).also {
            balloonMap[project] = it
            Disposer.register(it, Disposable {
                checkThread()
                balloonMap.remove(project, it)
            })

            it.show(tracker, position)
        }
    }

    /**
     * 显示对话框
     *
     * @return 对话框实例
     */
    fun showDialog(project: Project?): TranslationDialog {
        checkThread()

        val dialog = dialogMap[project] ?: TranslationDialog(project).also {
            dialogMap[project] = it
            Disposer.register(it, Disposable {
                checkThread()
                dialogMap.remove(project, it)
            })
        }

        return dialog.apply { show() }
    }

    /**
     * 显示状态栏图标
     */
    fun installStatusWidget(project: Project) {
        checkThread()
        TranslatorWidget(project).install()
    }

    /**
     * 关闭显示中的气泡和对话框
     */
    fun disposeUI(project: Project? = null) {
        checkThread()

        if (project == null) {
            balloonMap.values.toList().forEach { it.hide() }
            dialogMap.values.toList().forEach { it.close() }
        } else {
            balloonMap[project]?.hide()
            dialogMap[project]?.close()
        }
    }

    companion object {
        val instance: TranslationUIManager
            get() = ServiceManager.getService(TranslationUIManager::class.java)

        private fun checkThread() = checkDispatchThread(TranslationUIManager::class.java)
    }
}