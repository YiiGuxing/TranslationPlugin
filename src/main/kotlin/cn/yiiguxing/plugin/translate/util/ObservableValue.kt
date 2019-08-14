package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * An [ObservableValue] is an entity that wraps a value and allows to observe the value for changes.
 *
 * Created by Yii.Guxing on 2019/08/14.
 */
class ObservableValue<T>(initialValue: T) {

    private val listeners: MutableList<(newValue: T, oldValue: T) -> Unit> = ArrayList()

    var value: T by Delegates.observable(initialValue) { _, oldValue, newValue ->
        for (listener in listeners) {
            listener(newValue, oldValue)
        }
    }

    fun observe(parent: Disposable, listener: (newValue: T, oldValue: T) -> Unit) {
        listeners.add(listener)
        Disposer.register(parent, Disposable { listeners.remove(listener) })
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

}