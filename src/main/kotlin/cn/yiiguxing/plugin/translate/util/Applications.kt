/*
 * ApplicationUtils
 * 
 * Created by Yii.Guxing on 2017/12/22
 */
@file:Suppress("unused", "NOTHING_TO_INLINE", "MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.util

import cn.yiiguxing.plugin.translate.AppStorage
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TranslationUIManager
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import com.intellij.ide.IdeBundle
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.concurrent.Future

object App {

    const val PLUGIN_ID = "cn.yiiguxing.plugin.translate"

    val plugin: IdeaPluginDescriptor get() = PluginManager.getPlugin(PluginId.getId(PLUGIN_ID))!!

    val pluginVersion: String by lazy { plugin.version }

    val systemInfo: String by lazy {
        val appInfo = ApplicationInfoEx.getInstanceEx() as ApplicationInfoImpl

        val appName = appInfo.fullApplicationName

        var buildInfo = IdeBundle.message("about.box.build.number", appInfo.build.asString())
        val cal = appInfo.buildDate
        var buildDate = ""
        if (appInfo.build.isSnapshot) {
            buildDate = SimpleDateFormat("HH:mm, ").format(cal.time)
        }
        buildDate += DateFormatUtil.formatAboutDialogDate(cal.time)
        buildInfo += IdeBundle.message("about.box.build.date", buildDate)

        val properties = System.getProperties()
        val javaVersion = properties.getProperty("java.version", "unknown")
        val javaRuntimeVersion = properties.getProperty("java.runtime.version", javaVersion)
        val arch = properties.getProperty("os.arch", "")
        val jreInfo = IdeBundle.message("about.box.jre", javaRuntimeVersion, arch)

        val vmVersion = properties.getProperty("java.vm.name", "unknown")
        val vmVendor = properties.getProperty("java.vendor", "unknown")
        val jvmInfo = IdeBundle.message("about.box.vm", vmVersion, vmVendor)

        val pluginInfo = "Plugin v$pluginVersion"
        val osInfo = "OS: " + SystemInfo.getOsNameAndVersion()

        "$pluginInfo\n$appName\n$buildInfo\n$jreInfo\n$jvmInfo\n$osInfo"
    }
}

val AppStorage: AppStorage = AppStorage.instance
val Settings: Settings = Settings.instance
val TranslateService: TranslateService = TranslateService.instance
val TextToSpeech: TextToSpeech = TextToSpeech.instance
val TranslationUIManager: TranslationUIManager = TranslationUIManager.instance

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if
 * the current thread is not the Swing dispatch thread.
 */
inline fun checkDispatchThread(lazyMessage: () -> Any) {
    check(ApplicationManager.getApplication().isDispatchThread, lazyMessage)
}

/**
 * Throws an [IllegalStateException] with the [class][clazz] name if
 * the current thread is not the Swing dispatch thread.
 *
 * @see checkDispatchThread
 */
inline fun checkDispatchThread(clazz: Class<*>) = checkDispatchThread {
    "${clazz.simpleName} must only be used from the Event Dispatch Thread."
}

/**
 * Requests pooled thread to execute the [action].
 */
inline fun executeOnPooledThread(crossinline action: () -> Unit)
        : Future<*> = ApplicationManager.getApplication().executeOnPooledThread { action() }

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 */
inline fun invokeOnDispatchThread(crossinline action: () -> Unit) {
    with(ApplicationManager.getApplication()) {
        if (isDispatchThread) {
            action()
        } else {
            invokeLater { action() }
        }
    }
}

private val alarm: Alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread after the given [delay][delayMillis].
 */
fun invokeLater(delayMillis: Long, modalityState: ModalityState = ModalityState.any(), action: () -> Unit) {
    alarm.addRequest(action, delayMillis, modalityState)
}

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 */
inline fun invokeLater(crossinline action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater { action() }
}

/**
 * Asynchronously execute the [action] on the AWT event dispatching thread.
 *
 * @param state the state in which the runnable will be executed.
 */
inline fun invokeLater(state: ModalityState, crossinline action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater({ action() }, state)
}

/**
 * Shows the notification[Notification].
 */
fun Notification.show(project: Project? = null) {
    Notifications.Bus.notify(this, project)
}

/**
 * Copy the stack trace to clipboard.
 */
fun Throwable.copyToClipboard() {
    val stringWriter = StringWriter()
    PrintWriter(stringWriter).use {
        it.println(App.systemInfo.split("\n").joinToString(separator = "  \n>", prefix = ">"))
        it.println()

        it.println("```")
        printStackTrace(it)
        it.println("```")

        stringWriter.toString()
    }.let { CopyPasteManager.getInstance().setContents(StringSelection(it)) }
}
