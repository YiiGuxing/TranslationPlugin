@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.service

import cn.yiiguxing.plugin.translate.ui.InstantTranslationDialog
import cn.yiiguxing.plugin.translate.ui.TranslationBalloon
import cn.yiiguxing.plugin.translate.ui.TranslationDialog
import cn.yiiguxing.plugin.translate.ui.wordbook.WordOfTheDayDialog
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.checkDispatchThread
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.PositionTracker
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * TranslationUIManager
 */
class TranslationUIManager private constructor() {

    private val balloonMap: MutableMap<Project?, TranslationBalloon> = HashMap()
    private val dialogMap: MutableMap<Project?, TranslationDialog> = HashMap()
    private val instantTranslationDialogMap: MutableMap<Project?, InstantTranslationDialog> = HashMap()
    private val wordOfTheDayDialogMap: MutableMap<Project?, WordOfTheDayDialog> = HashMap()

    init {
        Application.messageBus.connect().subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
            override fun appClosing() = disposeUI()
        })
    }

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
        return showDialog(
            project,
            dialogMap
        ) { TranslationDialog(project) }
    }

    /**
     * 显示对话框
     *
     * @return 对话框实例
     */
    fun showInstantTranslationDialog(project: Project?): InstantTranslationDialog {
        return showDialog(
            project,
            instantTranslationDialogMap
        ) { InstantTranslationDialog(project) }
    }

    /**
     * 显示每日单词对话框
     *
     * @return 对话框实例
     */
    fun showWordOfTheDayDialog(project: Project?, words: List<WordBookItem>): WordOfTheDayDialog {
        return showDialog(
            project,
            wordOfTheDayDialogMap,
            { it.setWords(words) }) { WordOfTheDayDialog(project, words) }
    }

    /**
     * 关闭显示中的气泡和对话框
     */
    fun disposeUI(project: Project? = null) {
        checkThread()

        if (project == null) {
            for ((_, balloon) in balloonMap) {
                balloon.hide()
            }
            for ((_, dialog) in dialogMap) {
                dialog.close()
            }
            for ((_, dialog) in instantTranslationDialogMap) {
                dialog.close()
            }
        } else {
            balloonMap[project]?.hide()
            dialogMap[project]?.close()
            instantTranslationDialogMap[project]?.close()
        }

        LOGGER.d("Dispose project's UI: project=$project")
    }

    companion object {
        private val LOGGER: Logger = Logger.getInstance(TranslationUIManager::class.java)

        val instance: TranslationUIManager
            get() = ServiceManager.getService(TranslationUIManager::class.java)

        private fun checkThread() = checkDispatchThread(TranslationUIManager::class.java)

        private inline fun <D : DialogWrapper> showDialog(
            project: Project?,
            cache: MutableMap<Project?, D>,
            onBeforeShow: (D) -> Unit = {},
            dialog: () -> D
        ): D {
            checkThread()
            return cache.getOrPut(project) {
                dialog().also {
                    cache[project] = it
                    Disposer.register(it.disposable, Disposable {
                        checkThread()
                        cache.remove(project, it)
                    })
                }
            }.also {
                onBeforeShow(it)
                it.show()
            }
        }
    }
}