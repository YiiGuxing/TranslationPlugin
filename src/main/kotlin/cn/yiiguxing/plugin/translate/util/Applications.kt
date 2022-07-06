@file:Suppress("NOTHING_TO_INLINE", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.AppStorage
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import org.jetbrains.kotlin.idea.slicer.KotlinSliceUsageCellRenderer.action
import java.util.concurrent.Future

object Plugin {

    const val PLUGIN_ID = "cn.yiiguxing.plugin.translate"

    val descriptor: IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

    val version: String by lazy { descriptor.version }

}


inline val Application: Application get() = ApplicationManager.getApplication()

inline val AppStorage: AppStorage get() = AppStorage.instance
inline val Settings: Settings get() = Settings.instance
inline val TranslateService: TranslateService get() = TranslateService.instance
inline val CacheService: CacheService get() = CacheService.instance
inline val TextToSpeech: TextToSpeech get() = TextToSpeech.instance
inline val WordBookService: WordBookService get() = WordBookService.instance


/**
 * Asserts whether the method is being called from the event dispatch thread.
 */
inline fun assertIsDispatchThread() = Application.assertIsDispatchThread()

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if
 * the current thread is not the Swing dispatch thread.
 */
inline fun checkDispatchThread(lazyMessage: () -> Any) {
    check(Application.isDispatchThread, lazyMessage)
}

/**
 * Throws an [IllegalStateException] with the [class][clazz] name if
 * the current thread is not the Swing dispatch thread.
 *
 * @see checkDispatchThread
 */
inline fun checkDispatchThread(clazz: Class<*>) = checkDispatchThread {
    "${clazz.simpleName} must only be used from the Event Dispatch Thread."
}

/**
 * Requests pooled thread to execute the [action].
 */
inline fun executeOnPooledThread(crossinline action: () -> Unit)
        : Future<*> = Application.executeOnPooledThread { action() }

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 */
fun invokeLaterIfNeeded(
    state: ModalityState = ModalityState.defaultModalityState(),
    runnable: Runnable
) {
    with(Application) {
        if (isDispatchThread) {
            runnable.run()
        } else {
            Application.invokeLater(runnable, state)
        }
    }
}

/**
 * Causes `runnable()` to be executed synchronously on the AWT event dispatching
 * thread under Write Intent lock, when the IDE is in the specified modality
 * state (or a state with less modal dialogs open). This call blocks until all
 * pending AWT events have been processed and (then) `runnable()` returns.
 *
 * If current thread is an event dispatch thread then `runnable()`
 * is executed immediately regardless of the modality state.
 *
 * @param modalityState the state in which the runnable will be executed.
 * @param runnable      the runnable to execute.
 */
inline fun invokeAndWait(
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    crossinline runnable: () -> Unit
) {
    Application.invokeAndWait({ runnable() }, modalityState)
}

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 */
inline fun invokeLater(crossinline action: () -> Unit) {
    Application.invokeLater { action() }
}

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 *
 * @param state the state in which the runnable will be executed.
 */
inline fun invokeLater(state: ModalityState, crossinline action: () -> Unit) {
    Application.invokeLater({ action() }, state)
}

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 *
 * @param state the state in which the runnable will be executed.
 * @param expired condition to check before execution.
 */
inline fun invokeLater(
    state: ModalityState = ModalityState.defaultModalityState(),
    expired: Condition<*>,
    crossinline action: () -> Unit
) {
    Application.invokeLater({ action() }, state, expired)
}

/**
 * Shows the notification[Notification].
 */
fun Notification.show(project: Project? = null) {
    Notifications.Bus.notify(this, project)
}
