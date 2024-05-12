package cn.yiiguxing.plugin.translate.tts.microsoft

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.LogoHeaderPanel
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import org.jetbrains.concurrency.runAsync
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*

internal class EdgeTTSSettingsDialog : DialogWrapper(false) {

    private val settings = EdgeTTSSettings.instance()

    private val speedSlicer: JSlider = JSlider(
        EDGE_TTS_MIN_SPEED, EDGE_TTS_MAX_SPEED, settings.speed
    ).apply {
        majorTickSpacing = 25
        minorTickSpacing = 5
        snapToTicks = true
        paintTicks = true
        paintLabels = true
        labelTable = Hashtable<Int, JLabel>().apply {
            put(EDGE_TTS_MIN_SPEED, JLabel(message("tts.label.speed.slow")))
            put(EDGE_TTS_NORMAL_SPEED, JLabel(message("tts.label.speed.normal")))
            put(EDGE_TTS_MAX_SPEED, JLabel(message("tts.label.speed.fast")))
        }
    }

    private val voiceModel = MutableCollectionComboBoxModel<EdgeTTSVoice?>()
    private val voiceComboBox: ComboBox<EdgeTTSVoice?> = ComboBox(voiceModel)
    private val refreshButton: JButton = JButton(AllIcons.Actions.Refresh).apply {
        addActionListener { fetchVoiceList(true) }
    }
    private val errorText = JLabel().apply {
        isVisible = false
        foreground = JBColor.red
        icon = AllIcons.General.Error
    }

    init {
        title = message("microsoft.edge.tts.settings.dialog.title")
        setResizable(false)
        initVoiceComboBox()
        init()
    }

    private fun initVoiceComboBox() {
        val default = message("default")
        voiceComboBox.apply {
            renderer = SimpleListCellRenderer.create { label, voice, _ ->
                label.text = voice?.friendlyName ?: default
            }
        }

        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                if (voiceModel.size == 0 && settings.voice.isNullOrBlank()) {
                    fetchVoiceList()
                }
                return true
            }
        }.installOn(voiceComboBox)
    }

    override fun createCenterPanel(): JComponent {
        val form = createSettingsForm()
        return LogoHeaderPanel(TranslationIcons.load("image/microsoft_edge_tts_logo.svg"), form).apply {
            minimumSize = JBUI.size(MIN_WIDTH, 0)
        }
    }

    private fun createSettingsForm(): JComponent {
        val layout = UI.migLayout(
            UI.migSize(8),
            UI.migSize(8),
            insets = UI.migInsets(0, 8, 0, 0),
            lcBuilder = { hideMode(3) }
        )
        return JPanel(layout).apply {
            add(JLabel(message("tts.label.speed")))
            add(speedSlicer, UI.fillX().spanX(2).wrap())
            add(JLabel(message("tts.label.voice")))
            add(voiceComboBox, UI.fillX())
            add(refreshButton, UI.wrap())
            add(errorText, UI.spanX(3).maxWidth(UI.migSize(MIN_WIDTH - 50)))
        }
    }

    private fun updateVoices(voices: List<EdgeTTSVoice>, showPopup: Boolean) {
        val current = settings.voice
        val voiceList = ArrayList<EdgeTTSVoice?>(voices.size + 1).apply {
            add(null)
            addAll(voices)
        }

        voiceModel.update(voiceList)
        if (!current.isNullOrEmpty()) {
            voiceModel.selectedItem = voices.firstOrNull { it.name == current }
        }

        pack()
        if (showPopup) {
            voiceComboBox.showPopup()
        }
    }

    override fun setErrorText(text: String?) {
        errorText.text = text
        errorText.isVisible = !text.isNullOrEmpty()
        pack()
    }

    private fun fetchVoiceList(updateFocus: Boolean = false, autoShowPopup: Boolean = true) {
        refreshButton.disabledIcon = AnimatedIcon.Default.INSTANCE
        refreshButton.isEnabled = false
        voiceComboBox.isEnabled = false

        val dialogRef = DisposableRef.create(disposable, this)
        asyncLatch { latch ->
            runAsync {
                latch.await()
                if (updateFocus) {
                    EdgeTTSVoiceManager.fetchVoiceList()
                } else {
                    EdgeTTSVoiceManager.getVoices()
                }
            }
                .expireWith(disposable)
                .successOnUiThread(dialogRef) { dialog, voices ->
                    dialog.updateVoices(voices, autoShowPopup)
                    dialog.setErrorText(null)
                }
                .errorOnUiThread(dialogRef) { dialog, error ->
                    dialog.setErrorText(error.message ?: message("error.unknown"))
                }
                .finishOnUiThread(dialogRef) { dialog, _ ->
                    dialog.voiceComboBox.isEnabled = true
                    dialog.refreshButton.isEnabled = true
                    dialog.refreshButton.disabledIcon = null

                    dialogRef.dispose()
                }
        }
    }

    override fun doOKAction() {
        settings.apply {
            speed = speedSlicer.value
            voice = (voiceModel.selectedItem as? EdgeTTSVoice)?.name
        }

        super.doOKAction()
    }

    override fun show() {
        // This is a modal dialog, so it needs to be invoked later.
        SwingUtilities.invokeLater {
            if (!settings.voice.isNullOrBlank()) {
                fetchVoiceList(autoShowPopup = false)
            }
        }
        super.show()
    }

    companion object {
        private const val MIN_WIDTH = 700
    }
}