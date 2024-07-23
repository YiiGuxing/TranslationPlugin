package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class YoudaoSettingsDialog : DialogWrapper(true) {

    private val settings = service<YoudaoSettings>()

    // TODO 迁移到 Credential
    private val credentialSettings = Settings.getInstance().youdaoTranslateSettings

    private val appIdField: JBTextField = JBTextField()
    private val appKeyField: JBPasswordField = JBPasswordField()
    private val domainComboBox: ComboBox<YoudaoDomain> =
        ComboBox(CollectionComboBoxModel(YoudaoDomain.values().toList())).apply {
            renderer = SimpleListCellRenderer.create { label, domain, _ ->
                label.text = domain.displayName
            }
        }

    private var appKey: String?
        get() = appKeyField.password?.let { String(it) }
        set(value) {
            appKeyField.text = value
        }

    private var isAppKeySet: Boolean = false

    init {
        title = message("youdao.settings.dialog.title")
        isResizable = false

        init()

        appIdField.text = credentialSettings.appId
        domainComboBox.selected = settings.domain
    }

    override fun getHelpId(): String = HelpTopic.YOUDAO.id

    override fun createCenterPanel(): JComponent {
        val logo = TranslationIcons.load("/image/youdao_translate_logo.svg")
        val configurationPanel = createConfigurationPanel()
        return LogoHeaderPanel(logo, configurationPanel)
    }

    private fun createConfigurationPanel(): JPanel {
        val maxWidth = 400
        return JPanel(UI.migLayout(UI.migSize(8))).apply {
            maximumSize = JBUI.size(maxWidth, Integer.MAX_VALUE)

            add(JLabel(message("youdao.settings.dialog.label.domain")))
            add(
                ComponentPanelBuilder(domainComboBox)
                    .withTooltip(message("youdao.settings.dialog.label.domain.tip"))
                    .createPanel(), UI.wrap()
            )
            add(JLabel(message("youdao.settings.dialog.label.app.id")))
            add(appIdField, UI.fillX().wrap())
            add(JLabel(message("youdao.settings.dialog.label.app.key")))
            add(appKeyField, UI.fillX().wrap())

            val color = JBColor(0x7986CB, 0x757DE8).toRGBHex()
            add(
                UI.createHint(message("youdao.settings.dialog.hint", color), maxWidth),
                UI.spanX(2).gapTop(UI.migSize(16)).wrap()
            )
        }
    }

    override fun isOK(): Boolean = isAppKeySet && credentialSettings.appId.isNotEmpty()

    override fun doOKAction() {
        credentialSettings.setAppKey(appKey)
        credentialSettings.appId = appIdField.text.trim()
        isAppKeySet = credentialSettings.isAppKeySet

        updateDomain()
        super.doOKAction()
    }

    private fun updateDomain() {
        val oldDomain = settings.domain
        val newDomain = domainComboBox.selected ?: YoudaoDomain.GENERAL
        if (oldDomain != newDomain) {
            settings.domain = newDomain
            service<CacheService>().removeMemoryCache { key, _ ->
                key.translator == TranslationEngine.YOUDAO.id
            }
        }
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater {
            val dialogRef = DisposableRef.create(disposable, this)
            asyncLatch { latch ->
                runAsync {
                    latch.await()
                    credentialSettings.getAppKey() to credentialSettings.isAppKeySet
                }
                    .expireWith(disposable)
                    .successOnUiThread(dialogRef) { dialog, (appKey, isAppKeySet) ->
                        dialog.appKey = appKey
                        dialog.isAppKeySet = isAppKeySet
                    }
                    .disposeAfterProcessing(dialogRef)
            }
        }
        super.show()
    }
}