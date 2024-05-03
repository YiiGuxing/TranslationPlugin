package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackController
import cn.yiiguxing.plugin.translate.tts.sound.PlaybackStatus
import cn.yiiguxing.plugin.translate.tts.sound.isCompletedState
import cn.yiiguxing.plugin.translate.ui.icon.SoundIcon
import cn.yiiguxing.plugin.translate.util.Observable
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBDimension
import icons.TranslationIcons
import javax.swing.SwingConstants

/**
 * TTSButton
 */
class TTSButton(
    private val project: Project? = null
) : LinkLabel<Any>(), LinkListener<Any?>, Disposable {

    private var ttsController: PlaybackController? = null

    private lateinit var dataSource: () -> Pair<String, Lang>?

    init {
        myPaintUnderline = false
        toolTipText = message("tooltip.listen")
        horizontalAlignment = SwingConstants.CENTER
        preferredSize = JBDimension(16, 16)
        disabledIcon = SoundIcon.DISABLED

        resetIcon()
        setListener(this, null)
    }

    fun dataSource(ds: () -> Pair<String, Lang>?) {
        dataSource = ds
    }

    override fun linkSelected(aSource: LinkLabel<Any?>?, aLinkData: Any?) {
        toggle()
    }

    private fun resetIcon() {
        icon = SoundIcon.PASSIVE
        setHoveringIcon(SoundIcon.ACTIVE)
    }

    fun toggle() {
        if (!isEnabled) {
            return
        }

        ttsController?.let {
            it.stop()
            ttsController = null
            return
        }

        dataSource()?.let { (text, lang) ->
            if (text.isBlank()) {
                return
            }

            val loadingIcon = AnimatedIcon.Default()
            val playingIcon: SoundIcon by lazy { SoundIcon() }
            icon = loadingIcon
            setHoveringIcon(TranslationIcons.Stop)
            TextToSpeech.getInstance().speak(project, text, lang).let { controller ->
                ttsController = controller
                controller.statusBinding.observe(this, Observable.ChangeOnEDTListener { state, _ ->
                    when {
                        state == PlaybackStatus.PREPARING -> {
                            icon = loadingIcon
                        }

                        state == PlaybackStatus.PLAYING -> {
                            icon = playingIcon
                        }

                        state.isCompletedState -> {
                            ttsController = null
                            resetIcon()
                        }
                    }
                })
            }
        }
    }

    override fun dispose() {
        ttsController?.stop()
        ttsController = null
    }
}