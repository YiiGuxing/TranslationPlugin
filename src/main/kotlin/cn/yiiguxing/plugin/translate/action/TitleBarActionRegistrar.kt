package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.Plugin
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.diagnostic.Logger

class TitleBarActionRegistrar : AppLifecycleListener, DynamicPluginListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        if (IdeVersion >= IdeVersion.IDE2022_2) {
            registerAction()
        }
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (IdeVersion >= IdeVersion.IDE2022_2 && pluginDescriptor.pluginId.idString == Plugin.PLUGIN_ID) {
            registerAction()
        }
    }

    private fun registerAction() {
        try {
            val actionManager = ActionManager.getInstance() as ActionManagerImpl
            val group = actionManager.getAction(TITLE_BAR_ACTION_GROUP_ID) as? DefaultActionGroup ?: return
            val action = actionManager.getAction(TRANSLATION_TITLE_BAR_ACTION_ID) ?: return
            val clazz = group::class.java
            val method = clazz.getMethod("containsAction")
            method.isAccessible = true
            val isExist = method.invoke(group, action) as? Boolean ?: false
            if (!isExist) {
                actionManager.addToGroup(group, action, Constraints.FIRST)
            }
        } catch (e: Throwable) {
            LOG.w(e)
        }
    }

    companion object {
        private const val TRANSLATION_TITLE_BAR_ACTION_ID = "TranslationTitleBar"
        private const val TITLE_BAR_ACTION_GROUP_ID = "ExperimentalToolbarActions"

        private val LOG: Logger = Logger.getInstance(TitleBarActionRegistrar::class.java)
    }

}