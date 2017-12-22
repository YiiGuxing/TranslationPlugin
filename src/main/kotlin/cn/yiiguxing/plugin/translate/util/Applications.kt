/*
 * ApplicationUtils
 * 
 * Created by Yii.Guxing on 2017/12/22
 */
@file:Suppress("unused", "NOTHING_TO_INLINE")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.Future

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if
 * the current thread is not the Swing dispatch thread.
 */
inline fun checkDispatchThread(lazyMessage: () -> Any) {
    check(ApplicationManager.getApplication().isDispatchThread, lazyMessage)
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
        : Future<*> = ApplicationManager.getApplication().executeOnPooledThread { action() }

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 */
inline fun invokeLater(crossinline action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater { action() }
}