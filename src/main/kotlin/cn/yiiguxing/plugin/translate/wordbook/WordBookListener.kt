package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.util.messages.Topic

/**
 * WordBookChangeListener
 *
 * Created by Yii.Guxing on 2019/08/14.
 */
interface WordBookListener {

    /**
     * Called after the service is initialized.
     */
    fun onInitialized(service: WordBookService) {}

    /**
     * Called when a new [word][wordBookItem] is added.
     */
    fun onWordAdded(service: WordBookService, wordBookItem: WordBookItem) {}

    /**
     * Called when a [word][wordBookItem] is updated.
     */
    fun onWordUpdated(service: WordBookService, wordBookItem: WordBookItem) {}

    /**
     * Called when a word is removed.
     */
    fun onWordRemoved(service: WordBookService, id: Long) {}

    companion object {
        val TOPIC: Topic<WordBookListener> = Topic.create("WordBookChanged", WordBookListener::class.java)
    }
}