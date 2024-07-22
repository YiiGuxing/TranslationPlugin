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
    fun isObserved(listener: ChangeListener<T>): Boolean

    /**
     * Observe the change of the value.
     */
    fun observe(listener: ChangeListener<T>)

    /**
     * Observe the change of the value.
     */
    fun observe(parent: Disposable, listener: ChangeListener<T>)

    /**
     * Stop observing the change of the value.
     */
    fun unobserve(listener: ChangeListener<T>)

    /**
     * The change listener.
     */
    fun interface ChangeListener<T> {
        fun onChanged(newValue: T, oldValue: T)
    }

    open class ChangeOnEDTListener<T>(
        private val modalityState: ModalityState = ModalityState.defaultModalityState(),
        private val delegate: ChangeListener<T>
    ) : ChangeListener<T> {

        override fun onChanged(newValue: T, oldValue: T) {
            invokeLaterIfNeeded(modalityState) {
                delegate.onChanged(newValue, oldValue)
            }
        }
    }
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

abstract class AbstractObservable<T> : WritableObservable<T> {

    private val listeners: MutableList<Observable.ChangeListener<T>> = CopyOnWriteArrayList()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun isObserved(listener: Observable.ChangeListener<T>): Boolean {
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

    override fun observe(listener: Observable.ChangeListener<T>) {
        if (!isObserved(listener)) {
            listeners.add(listener)
        }
    }

    override fun observe(parent: Disposable, listener: Observable.ChangeListener<T>) {
        observe(listener)
        Disposer.register(parent) { unobserve(listener) }
    }

    override fun unobserve(listener: Observable.ChangeListener<T>) {
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

    fun asReadOnly(): Observable<T> = ReadOnlyWrapper(this)

    open class ReadOnlyWrapper<T>(private val wrapped: ObservableValue<T>) : Observable<T> by wrapped
}

/**
 * An [NotifyOnEDTObservableValue] is an entity that wraps a value
 * and allows to observe the value for changes on the EDT.
 */
open class NotifyOnEDTObservableValue<T>(
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