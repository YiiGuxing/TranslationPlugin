package cn.yiiguxing.plugin.translate.tts.sound.source

import cn.yiiguxing.plugin.translate.tts.sound.decode
import cn.yiiguxing.plugin.translate.util.withContextClassLoader
import javax.sound.sampled.AudioInputStream

/**
 * A [PlaybackSource] with context class loader.
 *
 * See also: [Using serviceloader](https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader)
 */
abstract class PlaybackSourceWithContext : PlaybackSource {

    @Suppress("MemberVisibilityCanBePrivate")
    var contextClassLoader: ClassLoader = javaClass.classLoader

    final override fun getAudioInputStream(): AudioInputStream {
        return withContextClassLoader(contextClassLoader) {
            getAudioInputStreamWithContext().decode()
        }
    }

    protected abstract fun getAudioInputStreamWithContext(): AudioInputStream

}