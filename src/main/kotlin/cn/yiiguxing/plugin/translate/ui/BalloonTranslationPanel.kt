package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import com.intellij.ui.layout.Row

/**
 * BalloonTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/13
 */
class BalloonTranslationPanel(settings: Settings, maxWidth: Int) : TranslationPanel(settings, maxWidth) {

    override val sourceLangRowInitializer: Row.() -> Unit = {

    }
    override val targetLangRowInitializer: Row.() -> Unit = {

    }
}