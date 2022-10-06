package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.util.messages.Topic

/**
 * WordBookChangeListener
 */
interface WordBookListener {

    /**
     * Called when the new [words] are added.
     *
     * @see WordBookService.addWord
     */
    fun onWordsAdded(service: WordBookService, words: List<WordBookItem>) {}

    /**
     * Called when the [words] are updated.
     *
     * @see WordBookService.updateWord
     */
    fun onWordsUpdated(service: WordBookService, words: List<WordBookItem>) {}

    /**
     * Called when the words are removed.
     *
     * @see WordBookService.removeWord
     * @see WordBookService.removeWords
     */
    fun onWordsRemoved(service: WordBookService, wordIds: List<Long>) {}

    /**
     * Called when the wordbook storage path is changed.
     */
    fun onStoragePathChanged(service: WordBookService) {}

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<WordBookListener> = Topic.create("WordBookListener", WordBookListener::class.java)
    }
}