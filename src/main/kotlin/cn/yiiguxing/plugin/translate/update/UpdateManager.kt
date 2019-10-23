package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.activity.Activity
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.Plugin
import cn.yiiguxing.plugin.translate.util.show
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
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
import java.awt.Color
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
        val version = plugin.version
        val displayId = "${plugin.name} Plugin Update"
        val title = "${plugin.name} plugin updated to v$version"
        val color = getBorderColor()
        val partStyle = "margin: ${JBUI.scale(8)}px 0;"
        val refStyle = "padding: ${JBUI.scale(3)}px ${JBUI.scale(6)}px; border-left: ${JBUI.scale(3)}px solid #$color;"
        val content = """
            If you find my plugin helpful, please
            <b><a href="$HTML_DESCRIPTION_SUPPORT">support me</a>.</b>
            If you love this plugin, please consider
            <b><a href="$HTML_DESCRIPTION_SUPPORT">donating</a></b> to sustain the plugin related activities.<br/>
            Thank you for your support!
            <div style="$partStyle $refStyle">
                This update addresses these <a href="${MILESTONE_URL.format(version)}">issues</a>.
            </div>
            Change notes:<br/>
            ${plugin.changeNotes}
        """.trimIndent()
        NotificationGroup(displayId, NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(
                title, content, NotificationType.INFORMATION,
                object : NotificationListener.Adapter() {
                    private val urlOpeningBehavior = NotificationListener.UrlOpeningListener(false)

                    override fun hyperlinkActivated(notification: Notification, hyperlinkEvent: HyperlinkEvent) {
                        if (hyperlinkEvent.description == HTML_DESCRIPTION_SUPPORT) {
                            SupportDialog.show()
                        } else {
                            urlOpeningBehavior.hyperlinkUpdate(notification, hyperlinkEvent)
                        }
                    }
                }
            )
            .addAction(SupportAction())
            .setImportant(true)
            .show(project)
    }

    class SupportAction : DumbAwareAction(message("support.notification"), null, Icons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private const val UPDATE_TOOL_WINDOW_ID = "Translation Assistant"

        private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

        private const val UPDATES_BASE_URL = "http://yiiguxing.github.io/TranslationPlugin/updates"

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=is%%3Aissue+milestone%%3Av%s+is%%3Aclosed"


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

        private class OpenInBrowserAction(private val versionUrl: String) :
            DumbAwareAction("在浏览器中打开", null, AllIcons.General.Web) {
            override fun actionPerformed(e: AnActionEvent?) = BrowserUtil.browse(versionUrl)
        }

        private class CloseAction(private val project: Project, private val toolWindow: ToolWindow) :
            DumbAwareAction("关闭", null, AllIcons.Actions.Close) {
            override fun actionPerformed(e: AnActionEvent?) = toolWindow.dispose(project)
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
                ).also { toolWindow ->
                    toolWindow.icon = AllIcons.Toolwindows.ToolWindowPalette
                    toolWindow.setAvailable(true, null)
                    toolWindow.isToHideOnEmptyContent = false
                    (toolWindow as ToolWindowEx).setTitleActions(
                        OpenInBrowserAction(versionUrl),
                        CloseAction(project, toolWindow)
                    )
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

        private fun getBorderColor(): String {
            val color = UIManager.getColor("DialogWrapper.southPanelDivider") ?: DEFAULT_BORDER_COLOR
            return (color.rgb and 0xffffff).toString(16)
        }

        private fun getHTMLEditorKit(): HTMLEditorKit {
            val htmlEditorKit = UIUtil.getHTMLEditorKit()
            val styleSheet = htmlEditorKit.styleSheet

            styleSheet.addRule("body { padding: ${JBUI.scale(8)}px; }")
            styleSheet.addRule("p { margin-top: ${JBUI.scale(8)}px; margin-bottom: ${JBUI.scale(6)}px; }")
            styleSheet.addRule("h2 { padding-top: ${JBUI.scale(15)}px; }")
            styleSheet.addRule("h3 { padding-top: ${JBUI.scale(8)}px; }")

            val color = getBorderColor()
            styleSheet.addRule(
                """
                    .hr {
                        border-color: #$color;
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
                        border-left: ${borderWidth}px solid #$color;
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
                "有道词典视图" to "全新的有道词典视图",
                "单词本单词标签" to "为单词本中的单词分组和归类",
                "单词本导入导出" to "实现单词本共享"
            )
        }

        private fun getUpdatedContent(version: String): String {
            fun imageResource(resource: String): String = "$UPDATES_BASE_URL/img/v$version/$resource"

            return """
                <h2>翻译</h2>
                <h3>有道词典视图</h3>
                <p>全新的有道词典视图，解析并结构化有道翻译的词典内容，使得有道翻译的词典内容与谷歌翻译的词典内容一样清晰易辩：</p>
                <img src="${imageResource("translation.gif")}" alt="全新的有道词典视图">
                
                <h2>单词本</h2>
                <h3>单词标签</h3>
                <p>现在，您可以为每一个单词指定一个或者一组标签，对其进行分组与归类：</p>
                <img src="${imageResource("group.gif")}" alt="单词分组">
                <p>编辑单词标签（使用逗号分隔多个标签）：</p>
                <img src="${imageResource("tags.gif")}" alt="编辑单词标签">
                <h3>导入导出</h3>
                <p>单词本现在可以导入或导出，实现单词本共享。单词本可以导出为以下格式：</p>
                <ul>
                    <li><b>JSON</b><i>（可用于单词本导入）</i></li>
                    <li><b>XML</b><i>（可用于单词本导入）</i></li>
                    <li><b>有道XML</b><i>（用于导入到有道词典，但不可用于单词本导入）</i></li>
                </ul>
                <img src="${imageResource("import_export.png")}" alt="导入导出">
            """.trimIndent()
        }
    }

}