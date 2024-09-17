package cn.yiiguxing.plugin.translate.update

import com.intellij.util.messages.Topic

interface UpdateListener {

    /**
     * Called after the update check is completed. If the update notification is shown,
     * this method will be called after the notification is closed.
     */
    fun onPostUpdate(hasUpdate: Boolean) {}

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<UpdateListener> = Topic.create("TranslationUpdateListener", UpdateListener::class.java)
    }
}