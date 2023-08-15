@file:Suppress("NOTHING_TO_INLINE", "MemberVisibilityCanBePrivate", "unused")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TranslationStates
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Condition
import java.util.concurrent.Future


inline val Application: Application get() = ApplicationManager.getApplication()

inline val TranslationStates: TranslationStates get() = TranslationStates.instance
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
 * Throws an [IllegalStateException] with the [class][T] name if
 * the current thread is not the Swing dispatch thread.
 *
 * @see checkDispatchThread
 */
inline fun <reified T> checkDispatchThread() = checkDispatchThread {
    "${T::class.java.simpleName} must only be used from the Event Dispatch Thread."
}

/**
 * Runs the specified read [action].
 * Can be called from any thread.
 * The action is executed immediately if no write action is currently running,
 * or blocked until the currently running write action completes.
 */
inline fun <T> runReadAction(crossinline action: () -> T): T = Application.runReadAction(Computable { action() })

/**
 * Runs the specified write [action].
 * Must be called from the Swing dispatch thread.
 * The action is executed immediately if no read actions are currently running,
 * or blocked until all read actions are complete.
 */
inline fun <T> runWriteAction(crossinline action: () -> T): T = Application.runWriteAction(Computable { action() })

/**
 * Requests pooled thread to execute the [action].
 */
inline fun executeOnPooledThread(crossinline action: () -> Unit)
        : Future<*> = Application.executeOnPooledThread { action() }

/**
 * Asynchronously execute the [runnable] on the AWT event dispatching thread.
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
 * Use this method when access any PSI, VirtualFiles, project/module model or other project settings, otherwise using
 * a corresponding method from UiUtil is allowed.
 *
 * Causes `runnable.run()` to be executed asynchronously on the
 * AWT event dispatching thread under Write Intent lock, when IDE is in the specified modality
 * state(or a state with less modal dialogs open) - unless the expiration condition is fulfilled.
 * This will happen after all pending AWT events have been processed.
 *
 * Please use this method instead of [javax.swing.SwingUtilities.invokeLater] or [com.intellij.util.ui.UIUtil] methods
 * for the reasons described in [ModalityState] documentation.
 * @param modalityState    the state in which the runnable will be executed.
 * @param expired  condition to check before execution.
 * @param action the action to execute.
 */
fun invokeLaterIfNeeded(
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    expired: Condition<*>,
    action: () -> Unit
) {
    val app = Application
    if (app.isDispatchThread) {
        action()
    } else {
        app.invokeLater(action, modalityState, expired)
    }
}

/**
 * Shows the notification[Notification].
 */
fun Notification.show(project: Project? = null) {
    Notifications.Bus.notify(this, project)
}
