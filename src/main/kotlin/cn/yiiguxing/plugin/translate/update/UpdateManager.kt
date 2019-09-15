package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.activity.Activity
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.Plugin
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerAdapter
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil
import icons.Icons
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLEditorKit

class UpdateManager : StartupActivity, DumbAware {

    override fun runActivity(project: Project) {
        if (Application.isUnitTestMode) {
            return
        }

        Activity.runLater(project, 3) {
            checkUpdate(project)
        }
    }

    private fun checkUpdate(project: Project) {
        val plugin = Plugin.descriptor
        val version = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val lastVersion = properties.getValue(VERSION_PROPERTY)
        if (version == lastVersion) {
            return
        }

        val versionParts = version.toVersionParts()
        val lastVersionParts = lastVersion?.toVersionParts()
        if (lastVersionParts != null && !versionParts.contentEquals(lastVersionParts)) {
            showUpdateToolWindow(project, versionParts)
        }

        showUpdateNotification(project, plugin)
        properties.setValue(VERSION_PROPERTY, version)
    }


    private fun showUpdateNotification(project: Project, plugin: IdeaPluginDescriptor) {
        val displayId = "${plugin.name} Plugin Update"
        val title = "${plugin.name} plugin updated to v${plugin.version}"
        val content = """
            If you find my plugin helpful, please
            <b><a href="$HTML_DESCRIPTION_SUPPORT">support me</a>:</b>
            <b><a href="$HTML_DESCRIPTION_SUPPORT">Donate</a></b> with
            <b><a href="$HTML_DESCRIPTION_SUPPORT">AliPay/WeChatPay</a>.</b><br/>
            Thank you for your support!<br/><br/>
            Change notes:<br/>
            ${plugin.changeNotes}
        """.trimIndent()
        NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(
                title, content, NotificationType.INFORMATION,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
                        if (hyperlinkEvent.description == HTML_DESCRIPTION_SUPPORT) {
                            SupportDialog.show()
                        } else {
                            URL_OPENING_LISTENER.hyperlinkUpdate(notification, hyperlinkEvent)
                        }
                    }
                }
            )
            .addAction(object : DumbAwareAction("Support!", null, Icons.Support) {
                override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
            })
            .setImportant(true)
            .show(project)
    }

    companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private const val UPDATE_TOOL_WINDOW_ID = "Translation Assistant"

        private val DEFAULT_BORDER_COLOR = JBColor(0xD0D0D0, 0x555555)

        private const val UPDATES_BASE_URL = "http://yiiguxing.github.io/TranslationPlugin/updates"


        private fun String.toVersionParts(): IntArray {
            val versionParts = split('.').take(2)
            return when (versionParts.size) {
                1 -> intArrayOf(versionParts[0].toInt(), 0)
                2 -> intArrayOf(versionParts[0].toInt(), versionParts[1].toInt())
                else -> throw IllegalStateException("Invalid version number: $this")
            }
        }

        fun showUpdateToolWindow(project: Project) {
            val versionParts = Plugin.descriptor.version.toVersionParts()
            showUpdateToolWindow(project, versionParts)
        }

        private fun showUpdateToolWindow(project: Project, versionParts: IntArray) {
            val version = versionParts.joinToString(".")
            val versionUrl = "$UPDATES_BASE_URL.html?v=$version"

            val toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project)
            val toolWindow = toolWindowManagerEx.getToolWindow(UPDATE_TOOL_WINDOW_ID)
                ?: toolWindowManagerEx.registerToolWindow(
                    UPDATE_TOOL_WINDOW_ID,
                    true,
                    ToolWindowAnchor.RIGHT,
                    project,
                    true,
                    false
                ).apply {
                    icon = AllIcons.Toolwindows.ToolWindowPalette
                    setAvailable(true, null)
                    isToHideOnEmptyContent = false
                    val openInBrowserAction = object : AnAction("在浏览器中打开", null, AllIcons.General.Web) {
                        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(versionUrl)
                    }
                    val closeAction = object : AnAction("关闭", null, AllIcons.Actions.Close) {
                        override fun actionPerformed(e: AnActionEvent) = dispose(project)
                    }
                    (this as ToolWindowEx).setTitleActions(openInBrowserAction, closeAction)
                }

            val contentManager = toolWindow.contentManager
            if (contentManager.contentCount == 0) {
                val contentComponent = createContentComponent(version, versionUrl)
                val content = ContentFactory.SERVICE.getInstance().createContent(contentComponent, "What's New", false)
                contentManager.addContent(content)
                contentManager.addContentManagerListener(object : ContentManagerAdapter() {
                    override fun contentRemoved(event: ContentManagerEvent) = toolWindow.dispose(project)
                })
            }

            toolWindow.show(null)
        }

        private fun ToolWindow.dispose(project: Project) {
            val toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project)
            toolWindowManagerEx.hideToolWindow(UPDATE_TOOL_WINDOW_ID, false)
            toolWindowManagerEx.unregisterToolWindow(UPDATE_TOOL_WINDOW_ID)
            Disposer.dispose(contentManager)
        }

        private fun getHTMLEditorKit(): HTMLEditorKit {
            val htmlEditorKit = UIUtil.getHTMLEditorKit()
            val styleSheet = htmlEditorKit.styleSheet

            styleSheet.addRule("body { padding: ${JBUI.scale(8)}px; }")
            styleSheet.addRule("p { margin-top: ${JBUI.scale(8)}px; margin-bottom: ${JBUI.scale(6)}px; }")
            styleSheet.addRule("h2 { padding-top: ${JBUI.scale(15)}px; }")
            styleSheet.addRule("h3 { padding-top: ${JBUI.scale(8)}px; }")

            val color = UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR
            val colorValue = (color.rgb and 0xffffff).toString(16)
            styleSheet.addRule(
                """
                    .hr {
                        border-color: #$colorValue;
                        border-style: solid;
                        border-width: 1px 0 0 0;
                        margin-top: ${JBUI.scale(8)}px;
                    }
                """.trimIndent()
            )

            val paddingLR = JBUI.scale(8)
            val paddingTB = JBUI.scale(4)
            val borderWidth = JBUI.scale(3)
            val margin = JBUI.scale(1)
            styleSheet.addRule(
                """
                    .reference {
                        margin: ${margin}px 0;
                        padding: ${paddingTB}px ${paddingLR}px;
                        border-left: ${borderWidth}px solid #$colorValue;
                    }
                """.trimIndent()
            )

            return htmlEditorKit
        }

        private fun createContentComponent(version: String, versionUrl: String): JComponent {
            val contentPane = JEditorPane()
            contentPane.isEditable = false
            contentPane.editorKit = getHTMLEditorKit()
            contentPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

            val keyHighlights = getKeyHighlights()
            contentPane.text = XmlStringUtil.wrapInHtml(
                """
                    <h1>What's New in $version</h1>
                    <div class="hr"></div>
                    <div>欢迎阅读v${version}的发行说明，此版本中有许多更新，希望您会喜欢，其中一些主要亮点包括：</div>
                    <ul>
                    ${keyHighlights.joinToString("\n") { "<li><b>${it.first}</b> - ${it.second}</li>" }}
                    </ul>
                    <div class="reference">如果您想在线阅读这些发行说明，请访问<a href="$versionUrl">这里</a>。</div>
                    <div class="reference">如果您想稍后再次打开此面板，请从主菜单中选择 <b>Help | What's New in Translation</b>.</div>
                    ${getUpdatedContent(version.replace('.', '_'))}
                """.trimIndent()
            )

            return JBScrollPane(contentPane).apply { border = JBUI.Borders.empty() }
        }

        private fun getKeyHighlights(): Array<Pair<String, String>> {
            return arrayOf(
                "发行说明" to "新功能一目了然",
                "单词本" to "新增了单词本功能，希望您会喜欢它"
            )
        }

        private fun getUpdatedContent(version: String): String {
            fun imageResource(resource: String): String = "$UPDATES_BASE_URL/img/v$version/$resource"

            return """
                <h2>发行说明</h2>
                <p>
                    现在，您可以通过阅读“<b>发行说明</b>”来了解某次更新中更新了哪些功能，并学习如何去使用这些功能。它将会在版本升级后弹出，如果您错过了，不用担心，您可以通过选择主菜单项
                    <b>Help | What's New in Translation</b> 重新打开它。
                </p>
                <img src="${imageResource("whats_new.png")}" alt="发行说明">
                
                <h2>单词本</h2>
                <img src="${imageResource("word_book.gif")}" style="margin-top: ${JBUI.scale(8)}px" alt="单词本">
                <p>某些IED（如：WebStorm）可能需要下载支持库才能使用“<b>单词本</b>”功能，这时候只需在“<b>单词本</b>”面板中点击“下载”并等待下载完成后即可正常使用。</p>
                <h3>每日单词</h3>
                <p>“<b>每日单词</b>”会将单词本中所有单词打乱顺序后逐个显示，可以通过 “<b>Word of the Day</b>” 动作（无默认快捷键）来打开它。</p>
                <img src="${imageResource("word_of_the_day.png")}" alt="每日单词">
                <p>也可以通过勾选设置页的 “启动时显示每日单词” 选项，使之在IDE启动后弹出显示。另外，“<b>每日单词</b>”默认会隐藏单词释义，如需取消隐藏，勾选设置页的 “默认显示单词释义” 选项即可。</p>
                <img src="${imageResource("word_of_the_day_opts.png")}" alt="每日单词选项">
            """.trimIndent()
        }
    }

}