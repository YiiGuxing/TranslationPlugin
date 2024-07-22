package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.TranslationPlugin

/**
 * Execute the [block] with the specified class loader.
 */
inline fun <T> withContextClassLoader(classLoader: ClassLoader, block: () -> T): T {
    val originalClassLoader = Thread.currentThread().contextClassLoader
    try {
        Thread.currentThread().contextClassLoader = classLoader
        return block()
    } finally {
        Thread.currentThread().contextClassLoader = originalClassLoader
    }
}

/**
 * Execute the [block] with the plugin's class loader.
 */
@Suppress("unused")
inline fun <T> withPluginContextClassLoader(block: () -> T): T {
    return withContextClassLoader(TranslationPlugin::class.java.classLoader, block)
}