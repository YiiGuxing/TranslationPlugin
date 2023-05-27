@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util.concurrent

import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.invokeAndWait
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.Promise
import java.util.function.Consumer


/**
 * Usage example:
 * ```
 * asyncLatch { latch ->
 *   runAsync {
 *     // Make sure to continue execution only
 *     // after the error handler has been successfully registered,
 *     // otherwise the default error handler may cause fatal errors in the IDE.
 *     latch.await()
 *     // ...
 *   }.onError { e ->
 *     // ...
 *   }
 * }
 * ```
 */
inline fun <T> asyncLatch(block: (Latch) -> Promise<T>): Promise<T> {
    val latch = AsyncLatch()
    return try {
        block(latch)
    } finally {
        latch.done()
    }
}

/**
 * Has no effect if the [Promise] is not a [CancellablePromise].
 */
fun <T> Promise<T>.expireWith(parentDisposable: Disposable): Promise<T> {
    if (this !is CancellablePromise) {
        return this
    }

    @Suppress("DEPRECATION")
    if (if (parentDisposable is Project) parentDisposable.isDisposed else Disposer.isDisposed(parentDisposable)) {
        cancel()
        return this
    }

    val childDisposable = Disposable { cancel() }
    Disposer.register(parentDisposable, childDisposable)
    return onProcessed { Disposer.dispose(childDisposable) }
}

fun <T> Promise<T>.successOnUiThread(
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (T) -> Unit
): Promise<T> = onUiThread(Promise<T>::onSuccess, modalityState, uiThreadAction)

fun <Ref, T> Promise<T>.successOnUiThread(
    disposableRef: DisposableRef<Ref>,
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (Ref & Any, T) -> Unit
): Promise<T> = onUiThread(Promise<T>::onSuccess, disposableRef, modalityState, uiThreadAction)

fun <T> Promise<T>.errorOnUiThread(
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (Throwable) -> Unit
): Promise<T> = onUiThread(Promise<T>::onError, modalityState, uiThreadAction)

fun <Ref, T> Promise<T>.errorOnUiThread(
    disposableRef: DisposableRef<Ref>,
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (Ref & Any, Throwable) -> Unit
): Promise<T> = onUiThread(Promise<T>::onError, disposableRef, modalityState, uiThreadAction)

fun <T> Promise<T>.finishOnUiThread(
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (T?) -> Unit
): Promise<T> = onUiThread(Promise<T>::onProcessed, modalityState, uiThreadAction)

fun <Ref, T> Promise<T>.finishOnUiThread(
    disposableRef: DisposableRef<Ref>,
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    uiThreadAction: (Ref & Any, T?) -> Unit
): Promise<T> = onUiThread(Promise<T>::onProcessed, disposableRef, modalityState, uiThreadAction)


internal inline fun <T, V> Promise<T>.onUiThread(
    fn: Promise<T>.(Consumer<V>) -> Promise<T>,
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    crossinline uiThreadAction: (V) -> Unit
): Promise<T> {
    return fn(Consumer { result ->
        invokeAndWait(modalityState) { uiThreadAction(result) }
    })
}

internal inline fun <Ref, T, V> Promise<T>.onUiThread(
    fn: Promise<T>.(Consumer<V>) -> Promise<T>,
    disposableRef: DisposableRef<Ref>,
    modalityState: ModalityState = ModalityState.defaultModalityState(),
    crossinline uiThreadAction: (Ref & Any, V) -> Unit
): Promise<T> {
    return expireWith(disposableRef)
        .fn(Consumer { result ->
            @Suppress("DEPRECATION")
            if (disposableRef.get() != null && !Disposer.isDisposed(disposableRef)) {
                invokeAndWait(modalityState) {
                    disposableRef.get()?.let { uiThreadAction(it, result) }
                }
            }
        })
}