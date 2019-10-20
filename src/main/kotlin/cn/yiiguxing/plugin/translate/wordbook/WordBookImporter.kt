package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import java.io.InputStream

interface WordBookImporter {

    fun import(input: InputStream, indicator: ProgressIndicator? = null) {
        indicator?.apply {
            checkCanceled()
            fraction = 0.0
            isIndeterminate = true
        }

        val words = input.readWords()
        val total = words.size
        if (total == 0) {
            return
        }

        indicator?.apply {
            checkCanceled()
            fraction = 0.0
            isIndeterminate = false
        }

        var success = 0
        val service = WordBookService
        for (i in words.indices) {
            indicator?.apply {
                checkCanceled()
                fraction = i.toDouble() / total.toDouble()
            }

            val isSuccessful = words[i].let { word ->
                try {
                    service.canAddToWordbook(word.word) && service.addWord(words[i], notifyOnFailed = false) != null
                } catch (e: Throwable) {
                    LOG.w("Failed to import word", e)
                    false
                }
            }

            if (isSuccessful) {
                success++
            }
        }

        LOG.d("Import completed, success: $success, total: $total.")

        indicator?.fraction = 1.0
    }

    fun InputStream.readWords(): List<WordBookItem>

    companion object {
        private val LOG = Logger.getInstance(WordBookImporter::class.java)
    }
}