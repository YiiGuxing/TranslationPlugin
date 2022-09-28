package cn.yiiguxing.plugin.translate

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object TranslationPlugin {

    const val PLUGIN_ID = "cn.yiiguxing.plugin.translate"

    val descriptor: IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

    val version: String by lazy { descriptor.version }

}