package cn.yiiguxing.plugin.translate.wordbook

import com.intellij.util.messages.Topic

interface WordBookViewListener {

    /**
     * Called when the wordbook is refreshed.
     *
     * @param words new words in the wordbook
     */
    fun onWordBookRefreshed(words: List<WordBookItem>) {}

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<WordBookViewListener> = Topic.create("WordBookViewListener", WordBookViewListener::class.java)
    }
}