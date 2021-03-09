package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.util.messages.Topic

interface RequireWordBookListener {

    fun onRequire() {}

    companion object {
        val TOPIC: Topic<RequireWordBookListener> =
            Topic.create("RequireWordBookListener", RequireWordBookListener::class.java)
    }
}