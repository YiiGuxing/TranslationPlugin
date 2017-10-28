package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.DEFAULT_USER_AGENT
import cn.yiiguxing.plugin.translate.GOOGLE_TTS
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.tk
import cn.yiiguxing.plugin.translate.util.urlEncode
import com.intellij.util.io.RequestBuilder

/**
 * GoogleTTSPlayer
 *
 * Created by Yii.Guxing on 2017-10-28 0028.
 */
class GoogleTTSPlayer(
        text: String,
        lang: Lang,
        private val listener: ((TTSPlayer) -> Unit)? = null
) : NetworkTTSPlayer(TTS_URL.format(lang.code, text.tk(), text.urlEncode())) {

    override fun buildRequest(builder: RequestBuilder) {
        builder.userAgent(DEFAULT_USER_AGENT)
    }

    override fun finished() {
        listener?.invoke(this)
    }

    companion object {
        private val TTS_URL = "$GOOGLE_TTS?client=gtx&ie=UTF-8&tl=%s&tk=%s&q=%s&prev=input&total=2&idx=1&textlen=152"
    }
}