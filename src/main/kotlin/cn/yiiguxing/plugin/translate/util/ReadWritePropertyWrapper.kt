package cn.yiiguxing.plugin.translate.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReadWritePropertyWrapper<in T, V>(private val propertyProvider: () -> ReadWritePropertyWrapper<T, V>) :
    ReadWriteProperty<T, V> {

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return propertyProvider().getValue(thisRef, property)
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        propertyProvider().setValue(thisRef, property, value)
    }
}