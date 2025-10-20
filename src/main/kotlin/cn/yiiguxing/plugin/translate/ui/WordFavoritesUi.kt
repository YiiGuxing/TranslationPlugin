package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.UI.setIcons
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookException
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import cn.yiiguxing.plugin.translate.wordbook.WordBookToolWindowFactory
import cn.yiiguxing.plugin.translate.wordbook.toWordBookItem
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.EDT
import icons.TranslationIcons
import java.lang.ref.WeakReference
import java.util.*

object WordFavoritesUi {

    private val favoriteDisposables: WeakHashMap<LinkLabel<*>, Disposable> = WeakHashMap()

    fun createStarLabel(): LinkLabel<Translation?> = LinkLabel<Translation?>().apply {
        updatePresentation(this, null)
    }

    fun updateStarLabel(
        project: Project?,
        starLabel: LinkLabel<Translation?>,
        translation: Translation?,
        parentDisposable: Disposable
    ) {
        EDT.assertIsEdt()
        favoriteDisposables.get(starLabel)?.let { Disposer.dispose(it) }
        updatePresentation(starLabel, translation?.favoriteId)

        starLabel.isEnabled = isEnable(project, translation)
        if (starLabel.isEnabled) {
            starLabel.setListener({ starLabel, _ ->
                toggleFavorite(project, starLabel, translation!!)
            }, translation)
        } else {
            starLabel.setListener(null, translation)
        }

        translation?.observableFavoriteId?.let {
            val favoriteDisposable = Disposable { favoriteDisposables.remove(starLabel) }
            Disposer.register(parentDisposable, favoriteDisposable)
            it.observe(favoriteDisposable) { favoriteId, _ ->
                updatePresentation(starLabel, favoriteId)
            }
            favoriteDisposables.put(starLabel, favoriteDisposable)
        }
    }

    private fun toolTipText(favoriteId: Long?): String {
        return when {
            !WordBookService.getInstance().isInitialized -> message("tooltip.wordBookNotInitialized")
            favoriteId == null -> message("tooltip.addToWordBook")
            else -> message("tooltip.removeFromWordBook")
        }
    }

    private fun isEnable(project: Project?, translation: Translation?): Boolean {
        val wordBookService = WordBookService.getInstance()
        return translation != null
                && (project?.isDisposed == false || wordBookService.isInitialized)
                && wordBookService.canAddToWordbook(translation.original)
    }

    private fun updatePresentation(starLabel: LinkLabel<*>, favoriteId: Long?) {
        val icon = if (favoriteId == null) TranslationIcons.StarOff else TranslationIcons.StarOn
        starLabel.setIcons(icon)
        starLabel.toolTipText = toolTipText(favoriteId)
    }

    private fun toggleFavorite(project: Project?, starLabel: LinkLabel<Translation?>, translation: Translation) {
        val wordBookService = WordBookService.getInstance()
        if (!wordBookService.isInitialized) {
            if (project != null) {
                WordBookToolWindowFactory.requireWordBook(project)
            } else {
                starLabel.isEnabled = false
            }
            return
        }

        starLabel.isEnabled = false
        if (!wordBookService.canAddToWordbook(translation.original)) {
            return
        }

        val starLabelRef = WeakReference(starLabel)
        val currentModalityState = ModalityState.current()
        executeOnPooledThread {
            val favoriteId = translation.favoriteId
            if (favoriteId == null) {
                val newFavoriteId = addToWordBook(translation)
                invokeLater(currentModalityState) {
                    if (translation.favoriteId == null) {
                        translation.favoriteId = newFavoriteId
                    }
                    starLabelRef.get()?.isEnabled = true
                }
            } else {
                removeWordFromWordBook(favoriteId)
                invokeLater(currentModalityState) { starLabelRef.get()?.isEnabled = true }
            }
        }
    }

    private fun addToWordBook(translation: Translation) = try {
        WordBookService.getInstance().addWord(translation.toWordBookItem())
    } catch (e: WordBookException) {
        Notifications.showErrorNotification(
            message("wordbook.notification.title"),
            message("wordbook.notification.message.word.addition.failed", e.errorCode.reason)
        )
        null
    }

    private fun removeWordFromWordBook(favoriteId: Long) = try {
        WordBookService.getInstance().removeWord(favoriteId)
    } catch (e: WordBookException) {
        Notifications.showErrorNotification(
            message("wordbook.notification.title"),
            message("wordbook.notification.message.operation.failed", e.errorCode.reason)
        )
    }
}