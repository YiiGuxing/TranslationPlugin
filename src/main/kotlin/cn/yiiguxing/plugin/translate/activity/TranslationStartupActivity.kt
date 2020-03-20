package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.service.StatusService
import cn.yiiguxing.plugin.translate.util.Application
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer

class TranslationStartupActivity : StartupActivity, DumbAware {

    override fun runActivity(project: Project) {
        if (Application.isUnitTestMode) return

        StatusService.getInstance(project).installStatusWidget()
        Disposer.register(project, Disposable { TranslationUIManager.disposeUI(project) })
    }

}