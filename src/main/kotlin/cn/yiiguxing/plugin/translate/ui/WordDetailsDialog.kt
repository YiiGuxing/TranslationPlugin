package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordDetailsDialogForm
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.util.Disposer

/**
 * Word details dialog.
 *
 * Created by Yii.Guxing on 2019/09/06.
 */
class WordDetailsDialog(word: WordBookItem) : WordDetailsDialogForm() {

    init {
        isModal = true
        title = message("word.details.title")
        Disposer.register(disposable, ttsButton)

        init()
    }

}