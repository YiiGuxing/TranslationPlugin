@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * An [Observable] is an entity that wraps a value
 * and allows to observe the value for changes.
 */
interface Observable<T> : ReadOnlyProperty<Any?, T> {

    val value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * Tests whether the specified [listener] has been observed.
     */
    fun isObserved(listener: ObservableListener<T>): Boolean

    /**
     * Observe the change of the value.
     */
    fun observe(listener: ObservableListener<T>)

    /**
     * Observe the change of the value.
     */
    fun observe(parent: Disposable, listener: ObservableListener<T>)

    /**
     * Stop observing the change of the value.
     */
    fun unobserve(listener: ObservableListener<T>)
}

/**
 * The change listener for [Observable].
 */
fun interface ObservableListener<T> {
    fun onChanged(newValue: T, oldValue: T)
}

/**
 * An [WritableObservable] is an entity that wraps a writable
 * value and allows to observe the value for changes.
 */
interface WritableObservable<T> : Observable<T>, ReadWriteProperty<Any?, T> {

    override var value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

}


/**
 * Creates a [WritableObservable] with the specified [initialValue].
 */
fun <T> observe(initialValue: T): WritableObservable<T> = ObservableValue(initialValue)

/**
 * Creates a read-only [Observable] for this [WritableObservable].
 */
fun <T> WritableObservable<T>.asReadOnly(): Observable<T> = ReadOnlyObservableWrapper(this)

/**
 * Creates a new [WritableObservable] that wraps this [WritableObservable]
 * and notifies the listeners on the EDT.
 */
fun <T> WritableObservable<T>.asEDTObservable(
    modalityState: ModalityState = ModalityState.defaultModalityState()
): WritableObservable<T> = EDTObservableWrapper(this, modalityState)


/**
 * An abstract implementation of [WritableObservable].
 */
abstract class AbstractObservable<T> : WritableObservable<T> {

    private val listeners: MutableList<ObservableListener<T>> = CopyOnWriteArrayList()

    override fun isObserved(listener: ObservableListener<T>): Boolean {
        return listener in listeners
    }

    /**
     * Called when the value changes.
     */
    protected open fun notifyChanged(oldValue: T, newValue: T) {
        for (listener in listeners) {
            listener.onChanged(newValue, oldValue)
        }
    }

    override fun observe(listener: ObservableListener<T>) {
        if (!isObserved(listener)) {
            listeners.add(listener)
        }
    }

    override fun observe(parent: Disposable, listener: ObservableListener<T>) {
        observe(listener)
        Disposer.register(parent) { unobserve(listener) }
    }

    override fun unobserve(listener: ObservableListener<T>) {
        listeners.remove(listener)
    }
}

private val DEFAULT_COMPARISON: (Any?, Any?) -> Boolean = { ov, nv -> ov != nv }

/**
 * An [ObservableValue] is an entity that wraps a value and allows to observe the value for changes.
 */
open class ObservableValue<T>(
    initialValue: T,
    private val comparison: (oldValue: T, newValue: T) -> Boolean = DEFAULT_COMPARISON
) : AbstractObservable<T>(), ReadWriteProperty<Any?, T> {

    @Volatile
    override var value = initialValue
        set(value) {
            val oldValue = field
            if (comparison(oldValue, value)) {
                field = value
                notifyChanged(oldValue, value)
            }
        }
}

/**
 * A read-only wrapper for a [WritableObservable].
 */
open class ReadOnlyObservableWrapper<T>(private val wrapped: WritableObservable<T>) : Observable<T> by wrapped

/**
 * A wrapper for [WritableObservable] that notifies the listeners on the EDT.
 */
open class EDTObservableWrapper<T>(
    private val wrapped: WritableObservable<T>,
    private val modalityState: ModalityState
) : AbstractObservable<T>() {

    override var value: T
        get() = wrapped.value
        set(value) {
            wrapped.value = value
        }

    init {
        var listener = ObservableListener<T> { newValue, oldValue -> notifyChanged(oldValue, newValue) }
        if (wrapped !is EDTObservableWrapper || wrapped.modalityState != modalityState) {
            listener = EDTObservableListener(modalityState, listener)
        }
        wrapped.observe(listener)
    }
}

/**
 * An [EDTObservableValue] is a subclass of [ObservableValue] that notifies the listeners on the EDT.
 */
open class EDTObservableValue<T>(
    initialValue: T,
    private val modalityState: ModalityState = ModalityState.defaultModalityState(),
    comparison: (oldValue: T, newValue: T) -> Boolean = DEFAULT_COMPARISON
) : ObservableValue<T>(initialValue, comparison) {
    override fun notifyChanged(oldValue: T, newValue: T) {
        invokeLaterIfNeeded(modalityState) {
            super.notifyChanged(oldValue, newValue)
        }
    }
}


/**
 * Executes value change callbacks on EDT.
 */
class EDTObservableListener<T>(
    private val modalityState: ModalityState = ModalityState.defaultModalityState(),
    private val delegate: ObservableListener<T>
) : ObservableListener<T> {
    override fun onChanged(newValue: T, oldValue: T) {
        invokeLaterIfNeeded(modalityState) {
            delegate.onChanged(newValue, oldValue)
        }
    }
}