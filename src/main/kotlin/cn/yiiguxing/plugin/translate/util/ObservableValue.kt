package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * An [Observable] is an entity that wraps a value and allows to observe the value for changes.
 */
interface Observable<T> : ReadOnlyProperty<Any?, T> {

    val value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /**
     * Tests whether the specified [listener] has been observed.
     */
    fun isObserved(listener: (newValue: T, oldValue: T) -> Unit): Boolean

    /**
     * Observe the change of the value.
     */
    fun observe(parent: Disposable, listener: (newValue: T, oldValue: T) -> Unit)
}

abstract class AbstractObservable<T> : Observable<T> {

    private val listeners: MutableList<(newValue: T, oldValue: T) -> Unit> = CopyOnWriteArrayList()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun isObserved(listener: (newValue: T, oldValue: T) -> Unit): Boolean {
        return listener in listeners
    }

    /**
     * Called when the value changes.
     */
    protected open fun notifyChanged(oldValue: T, newValue: T) {
        for (listener in listeners) {
            listener(newValue, oldValue)
        }
    }

    override fun observe(parent: Disposable, listener: (newValue: T, oldValue: T) -> Unit) {
        if (!isObserved(listener)) {
            listeners.add(listener)
        }
        Disposer.register(parent) { listeners.remove(listener) }
    }
}

/**
 * An [ObservableValue] is an entity that wraps a value and allows to observe the value for changes.
 */
open class ObservableValue<T>(
    initialValue: T,
    private val comparison: (oldValue: T, newValue: T) -> Boolean = { ov, nv -> ov != nv }
) : AbstractObservable<T>(), ReadWriteProperty<Any?, T> {

    override var value = initialValue
        set(value) {
            val oldValue = field
            if (comparison(oldValue, value)) {
                field = value
                notifyChanged(oldValue, value)
            }
        }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun asReadOnly(): Observable<T> = ReadOnlyWrapper(this)

    open class ReadOnlyWrapper<T>(private val wrapped: ObservableValue<T>) : Observable<T> by wrapped
}