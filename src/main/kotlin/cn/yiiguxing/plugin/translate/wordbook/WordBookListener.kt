package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.util.messages.Topic

/**
 * WordBookChangeListener
 */
interface WordBookListener {

    /**
     * Called when the new [words] are added.
     */
    fun onWordsAdded(service: WordBookService, words: List<WordBookItem>) {}

    /**
     * Called when the [words] are updated.
     */
    fun onWordsUpdated(service: WordBookService, words: List<WordBookItem>) {}

    /**
     * Called when the words are removed.
     */
    fun onWordsRemoved(service: WordBookService, wordIds: List<Long>) {}

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<WordBookListener> = Topic.create("WordBookListener", WordBookListener::class.java)
    }
}