package cn.yiiguxing.plugin.translate.activity

import cn.yiiguxing.plugin.translate.service.StatusService
import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class TranslationStartupActivity : StartupActivity, DumbAware {

    override fun runActivity(project: Project) {
        if (Application.isUnitTestMode) return

        StatusService.getInstance(project).installStatusWidget()
    }

}