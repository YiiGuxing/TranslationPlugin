package cn.yiiguxing.plugin.translate

import com.intellij.AbstractBundle
import com.intellij.DynamicBundle
import java.util.*

abstract class MyDynamicBundle(bundle: String) : AbstractBundle(bundle) {
    override fun findBundle(
        pathToBundle: String,
        loader: ClassLoader,
        control: ResourceBundle.Control
    ): ResourceBundle {
        val dynamicBundleLocale = DynamicBundle.getLocale()
        val dynamicBundle = ResourceBundle.getBundle(pathToBundle, dynamicBundleLocale, loader, control)
        return dynamicBundle ?: super.findBundle(pathToBundle, loader, control)
    }
}