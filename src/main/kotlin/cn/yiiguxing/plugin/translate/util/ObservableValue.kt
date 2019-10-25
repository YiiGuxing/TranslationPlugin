package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * An [ObservableValue] is an entity that wraps a value and allows to observe the value for changes.
 */
class ObservableValue<T>(
    initialValue: T,
    private val comparison: (oldValue: T, newValue: T) -> Boolean = { ov, nv -> ov != nv }
) : ObservableProperty<T>(initialValue) {

    private val listeners: MutableList<(newValue: T, oldValue: T) -> Unit> = CopyOnWriteArrayList()

    var value: T by this

    override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
        return comparison(oldValue, newValue)
    }

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        for (listener in listeners) {
            listener(newValue, oldValue)
        }
    }

    fun observe(parent: Disposable, listener: (newValue: T, oldValue: T) -> Unit) {
        listeners.add(listener)
        Disposer.register(parent, Disposable { listeners.remove(listener) })
    }

}