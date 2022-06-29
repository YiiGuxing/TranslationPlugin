package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.Plugin
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionStub
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl

class TitleBarActionRegistrar : AppLifecycleListener, DynamicPluginListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        registerAction()
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        registerAction()
    }

    private fun registerAction() {
        val actionManager = ActionManager.getInstance() as ActionManagerImpl
        val action = actionManager.getAction(TRANSLATION_TITLE_BAR_ACTION_ID) ?: return
        val name = if (action is ActionStub) action.className else action.javaClass.name
        val group = actionManager.getParentGroup(TITLE_BAR_ACTION_GROUP_ID, name, Plugin.descriptor) ?: return
        actionManager.addToGroup(group, action, Constraints.FIRST)
    }

    companion object {
        private const val TRANSLATION_TITLE_BAR_ACTION_ID = "TranslationTitleBar"
        private const val TITLE_BAR_ACTION_GROUP_ID = "ExperimentalToolbarActions"
    }

}