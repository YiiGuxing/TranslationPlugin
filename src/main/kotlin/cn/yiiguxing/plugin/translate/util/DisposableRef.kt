package cn.yiiguxing.plugin.translate.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref

/**
 * Disposable reference, the referenced value will be eliminated on disposed.
 */
class DisposableRef<T> private constructor(value: T? = null) : Ref<T>(value), Disposable {

    override fun dispose() {
        set(null)
    }

    fun disposeSelf() {
        Disposer.dispose(this)
    }

    companion object {
        fun <T> create(value: T? = null): DisposableRef<T> {
            return DisposableRef(value)
        }

        fun <T> create(parentDisposable: Disposable, value: T? = null): DisposableRef<T> {
            return create(value).also { Disposer.register(parentDisposable, it) }
        }
    }
}