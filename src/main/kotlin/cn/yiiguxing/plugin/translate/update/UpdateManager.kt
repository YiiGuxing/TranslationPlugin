package cn.yiiguxing.plugin.translate.update

import cn.yiiguxing.plugin.translate.HTML_DESCRIPTION_SUPPORT
import cn.yiiguxing.plugin.translate.activity.BaseStartupActivity
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import cn.yiiguxing.plugin.translate.util.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
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
import com.intellij.util.io.HttpRequests
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

class UpdateManager : BaseStartupActivity(), DumbAware {

    override fun onRunActivity(project: Project) {
        checkUpdate(project)
        checkUpdateFromGithub(project)
    }

    private fun checkUpdate(project: Project) {
        val plugin = Plugin.descriptor
        val versionString = plugin.version
        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val lastVersionString = properties.getValue(VERSION_PROPERTY, "0.0")
        if (versionString == lastVersionString) {
            return
        }

        val version = Version(versionString)
        val lastVersion = Version(lastVersionString)
        if (version > lastVersion) {
            showUpdateToolWindow(project, version)
        }

        showUpdateNotification(project, plugin)
        properties.setValue(VERSION_PROPERTY, versionString)
    }

    private fun checkUpdateFromGithub(project: Project) {
        if (IdeVersion.isIde2019_3OrNewer) return

        executeOnPooledThread {
            val newVersion = try {
                HttpRequests.request(UPDATES_API)
                    .connect { Gson().fromJson(it.readString(null), Version::class.java) }!!
            } catch (e: Throwable) {
                LOGGER.w("Cannot get release info from Github.", e)
                return@executeOnPooledThread
            }

            LOGGER.d("Latest released plugin version: $newVersion")

            if (newVersion.versionNumbers.first >= 3) {
                val properties: PropertiesComponent = PropertiesComponent.getInstance()
                val lastVersionString = properties.getValue(VERSION_IN_GITHUB_PROPERTY, "0.0")
                val lastVersion = Version(lastVersionString)

                if (newVersion > lastVersion) {
                    invokeOnDispatchThread {
                        if (!project.isDisposed) {
                            showUpdateIDENotification(project, newVersion)
                        }
                    }
                }

                properties.setValue(VERSION_IN_GITHUB_PROPERTY, newVersion.versionString)
            }
        }
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

    private fun showUpdateIDENotification(project: Project, version: Version) {
        NotificationGroup("Translation Plugin Update(IDE)", NotificationDisplayType.STICKY_BALLOON, false)
            .createNotification(
                message("updater.v3.notification.title", version.versionString),
                message("updater.v3.notification.content", version.versionString),
                NotificationType.INFORMATION,
                null
            )
            .addAction(SupportAction())
            .addAction(UpdateDetailsAction(version))
            .setImportant(true)
            .show(project)
    }

    private class SupportAction : DumbAwareAction(message("support.notification"), null, Icons.Support) {
        override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
    }

    private class UpdateDetailsAction(private val version: Version) :
        DumbAwareAction(message("updater.v3.notification.action.detail")) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(version.updatesUrl)
    }

    private class OpenInBrowserAction(private val versionUrl: String) :
        DumbAwareAction(message("updater.notification.action.read.in.a.browser"), null, AllIcons.General.Web) {
        override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(versionUrl)
    }

    private class CloseAction(private val project: Project, private val toolWindow: ToolWindow) :
        DumbAwareAction(message("updater.notification.action.close"), null, AllIcons.Actions.Close) {
        override fun actionPerformed(e: AnActionEvent) = toolWindow.dispose(project)
    }

    data class Version @JvmOverloads constructor(@SerializedName("tag_name") val version: String = "v0.0") {

        val versionNumbers: Pair<Int, Int> by lazy { version.toVersionParts() }

        val versionString: String by lazy { "${versionNumbers.first}.${versionNumbers.second}" }

        override fun toString(): String = "Version(version=$version, versionNumbers=$versionNumbers)"

        operator fun compareTo(other: Version): Int {
            val compare = versionNumbers.first.compareTo(other.versionNumbers.first)
            return if (compare == 0) versionNumbers.second.compareTo(other.versionNumbers.second) else compare
        }

    }

    companion object {
        private const val VERSION_PROPERTY = "${Plugin.PLUGIN_ID}.version"

        private const val VERSION_IN_GITHUB_PROPERTY = "${Plugin.PLUGIN_ID}.version.github"

        private const val UPDATE_TOOL_WINDOW_ID = "Translation Assistant"

        private val DEFAULT_BORDER_COLOR: Color = JBColor(0xD0D0D0, 0x555555)

        private const val UPDATES_BASE_URL = "http://yiiguxing.github.io/TranslationPlugin/updates"

        private const val MILESTONE_URL =
            "https://github.com/YiiGuxing/TranslationPlugin/issues?q=milestone%%3Av%s+is%%3Aclosed"

        private const val UPDATES_API = "https://api.github.com/repos/YiiGuxing/TranslationPlugin/releases/latest"

        private val LOGGER: Logger = Logger.getInstance(UpdateManager::class.java)


        private fun String.toVersionParts(): Pair<Int, Int> {
            val versionString = if (this[0].equals('v', true)) substring(1) else this
            val versionParts = versionString.split('.', '-').take(2)
            return when (versionParts.size) {
                1 -> versionParts[0].toInt() to 0
                2 -> versionParts[0].toInt() to versionParts[1].toInt()
                else -> throw IllegalStateException("Invalid version number: $this")
            }
        }

        private val Version.updatesUrl: String get() = "$UPDATES_BASE_URL.html?v=$versionString"

        fun showUpdateToolWindow(project: Project) {
            showUpdateToolWindow(project, Version(Plugin.descriptor.version))
        }

        private fun showUpdateToolWindow(project: Project, version: Version) {
            val versionUrl = version.updatesUrl
            val toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project)
            val toolWindow = toolWindowManagerEx.getToolWindow(UPDATE_TOOL_WINDOW_ID)
                ?: toolWindowManagerEx.registerToolWindow(
                    UPDATE_TOOL_WINDOW_ID,
                    true,
                    ToolWindowAnchor.RIGHT,
                    project,
                    true,
                    false
                )

            toolWindow as ToolWindowEx
            toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowPalette)
            toolWindow.setAvailable(true, null)
            toolWindow.setToHideOnEmptyContent(false)
            toolWindow.setTitleActions(
                OpenInBrowserAction(versionUrl),
                CloseAction(project, toolWindow)
            )

            val contentManager = toolWindow.contentManager
            if (contentManager.contentCount == 0) {
                val contentComponent = createContentComponent(version.versionString, versionUrl)
                val content = ContentFactory.SERVICE.getInstance().createContent(contentComponent, "What's New", false)
                contentManager.addContent(content)
                contentManager.addContentManagerListener(object : ContentManagerAdapter() {
                    override fun contentRemoved(event: ContentManagerEvent) {
                        contentManager.removeContentManagerListener(this)
                        toolWindow.dispose(project)
                    }
                })
            }

            toolWindow.show(null)
        }

        private fun ToolWindow.dispose(project: Project) {
            val toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project)
            toolWindowManagerEx.hideToolWindow(UPDATE_TOOL_WINDOW_ID, false)
            if (toolWindowManagerEx.getToolWindow(UPDATE_TOOL_WINDOW_ID) != null) {
                toolWindowManagerEx.unregisterToolWindow(UPDATE_TOOL_WINDOW_ID)
            }
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
                    <div>欢迎使用<b>Translation v${version}</b>，此版本中包含了以下主要更新：</div>
                    <ul>
                    ${keyHighlights.joinToString("\n") { "<li><b>${it.first}</b> - ${it.second}</li>" }}
                    </ul>
                    <div class="reference">如果您想在线阅读此发行说明，请访问<a href="$versionUrl">这里</a>。</div>
                    <div class="reference">如果您想稍后再次打开此面板，请从主菜单中选择 <b>Help | What's New in Translation</b>.</div>
                    ${getUpdatedContent(version.replace('.', '_'))}
                """.trimIndent()
            )

            return JBScrollPane(contentPane).apply { border = JBUI.Borders.empty() }
        }

        private fun getKeyHighlights(): Array<Pair<String, String>> {
            return arrayOf(
                "Quick Documentation翻译" to "支持对Quick Documentation进行翻译",
                "单词本txt文本导出" to "单词本支持导出为txt文本"
            )
        }

        private fun getUpdatedContent(version: String): String {
            fun imageResource(resource: String): String = "$UPDATES_BASE_URL/img/v$version/$resource"

            return """
                <h2>翻译</h2>
                <h3>Quick Documentation翻译</h3>
                <p><i>感谢 <a href="https://github.com/niktrop" target="_blank">Nikolay Tropin</a>(来自 <a href="https://www.jetbrains.com" target="_blank">JetBrains</a>) 提供的功能实现！</i></p>
                <p>我们增加了对 <i>Quick Documentation</i> 的翻译的支持。现在，你可以随时随地对代码中的文档进行翻译，而不是受限于源代码中的文档注释。例如在 Windows 平台中，当你使用 Ctrl+Q 查看 <i>Quick Documentation</i> 时，你得到的将是已翻译好的文档。</p>
                <p><img src="${imageResource("quick_doc1.gif")}" alt="Quick Documentation"></p>
                <p><img src="${imageResource("quick_doc2.gif")}" alt="Quick Documentation"></p>
                <p><i>Quick Documentation</i> 翻译选项是默认开启的，如果你想关闭此选项，可到在插件配置页面进行关闭。</p>
                <p><img src="${imageResource("quick_doc_opt.png")}" alt="翻译文档选项"></p>
            
                <h2>单词本</h2>
                <h3>导出为txt文本</h3>
                <img src="${imageResource("wordbook_exporter.png")}" alt="单词本">
                <p><i>感谢 <a href="https://github.com/kaiattrib" target="_blank">Kaiattrib</a> 提供的功能实现！</i></p>
            """.trimIndent()
        }
    }

}