package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware

/**
 * 显示翻译对话框动作
 *
 * Created by Yii.Guxing on 2017/9/11
 */
class ShowTranslationDialogAction : AnAction(), DumbAware {

    init {
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment)
            return

        //TranslationManager.instance.showDialog(e.project)
        TextToSpeech.INSTANCE.speak("Almost a decade ago, GitHub was created as a place for developers to work together on code. Now, millions of people around the world use our platform to build businesses, learn from each other, and create tools we’ll use for decades to come. Together, you’ve shown that some of the most inventive, impactful things happen when curious and creative people have a space to work together. Today, at GitHub Universe, we shared plans to build on our ten years of experience and 1.5 billion commits. We've taken the first step toward using the world's largest collection of open source data to improve the way we collaborate with these new experiences.")
    }
}