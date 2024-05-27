package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry

class TitleBarActionRegistrar : AppLifecycleListener, DynamicPluginListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        if (isNewUI) {
            registerAction()
        }
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (isNewUI && pluginDescriptor.pluginId.idString == TranslationPlugin.PLUGIN_ID) {
            registerAction()
        }
    }

    private fun registerAction() {
        try {
            val (targetGroupId, constraints) = if (IdeVersion >= IdeVersion.IDE2022_3) {
                MAIN_TOOLBAR_RIGHT_GROUP_ID to Constraints(Anchor.BEFORE, SEARCH_EVERYWHERE_ACTION_ID)
            } else {
                TITLE_BAR_ACTION_GROUP_ID to Constraints.FIRST
            }
            val actionManager = ActionManager.getInstance() as ActionManagerImpl
            val group = actionManager.getAction(targetGroupId) as? DefaultActionGroup ?: return
            val action = actionManager.getAction(TRANSLATION_TITLE_BAR_ACTION_ID) ?: return
            if (!group.containsAction(action)) {
                actionManager.addToGroup(group, action, constraints)
            }
        } catch (e: Throwable) {
            LOG.w(e)
        }
    }

    companion object {
        private const val TRANSLATION_TITLE_BAR_ACTION_ID = "TranslationTitleBar"
        private const val TITLE_BAR_ACTION_GROUP_ID = "ExperimentalToolbarActions"
        private const val MAIN_TOOLBAR_RIGHT_GROUP_ID = "MainToolbarRight"
        private const val SEARCH_EVERYWHERE_ACTION_ID = "SearchEverywhere"

        private val LOG: Logger = Logger.getInstance(TitleBarActionRegistrar::class.java)

        private val isNewUI: Boolean
            get() = IdeVersion >= IdeVersion.IDE2022_2 && Registry.`is`("ide.experimental.ui", true)
    }

}