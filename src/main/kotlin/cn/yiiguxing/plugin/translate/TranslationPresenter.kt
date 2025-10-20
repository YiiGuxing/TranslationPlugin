package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.LanguageSelection.*
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.tts.TextToSpeech
import cn.yiiguxing.plugin.translate.util.NON_LATIN_CONDITION
import cn.yiiguxing.plugin.translate.util.any
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.diagnostic.Logger
import java.lang.ref.WeakReference

class TranslationPresenter(private val view: View, private val recordHistory: Boolean = true) : Presenter {

    private val translateService = TranslateService.getInstance()
    private val settings = Settings.getInstance()
    private val states = TranslationStates.getInstance()
    private var currentRequest: Presenter.Request? = null

    override val translator: Translator
        get() = translateService.translator

    override val histories: List<String> get() = states.getHistories()

    override val primaryLanguage: Lang get() = translator.primaryLanguage

    override val supportedLanguages: SupportedLanguagesData
        get() = with(translator) {
            SupportedLanguagesData(supportedSourceLanguages, supportedTargetLanguages)
        }

    override val isExplicitSourceLanguage: Boolean
        get() = when (settings.sourceLanguageSelection) {
            null -> settings.sourceLanguage?.isExplicit() ?: false
            LAST_USED -> states.lastLanguages.source.isExplicit()
            PRIMARY -> true
            else -> false
        }

    override val isExplicitTargetLanguage: Boolean
        get() = when (settings.targetLanguageSelection) {
            null -> settings.targetLanguage?.isExplicit() ?: false
            LAST_USED -> states.lastLanguages.target.isExplicit()
            PRIMARY -> true
            else -> false
        }

    override fun isSupportedSourceLanguage(sourceLanguage: Lang): Boolean {
        return translator.supportedSourceLanguages.contains(sourceLanguage)
    }

    override fun isSupportedTargetLanguage(targetLanguage: Lang): Boolean {
        return translator.supportedTargetLanguages.contains(targetLanguage)
    }

    override fun getCache(text: String, srcLang: Lang, targetLang: Lang): Translation? {
        return translateService.getCache(text, srcLang, targetLang)
    }

    override fun getSourceLang(text: String): Lang {
        return when (settings.sourceLanguageSelection) {
            MAIN_OR_ENGLISH -> if (text.isNotBlank() && text.any(NON_LATIN_CONDITION)) {
                primaryLanguage
            } else {
                Lang.ENGLISH
            }

            PRIMARY -> primaryLanguage
            LAST_USED -> states.lastLanguages.source
            else -> settings.sourceLanguage ?: Lang.AUTO
        }.takeIf { isSupportedSourceLanguage(it) } ?: primaryLanguage
    }

    override fun getTargetLang(sourceLanguage: Lang, text: String): Lang {
        return when (settings.targetLanguageSelection) {
            MAIN_OR_ENGLISH -> {
                val sourceIsEnglish = sourceLanguage == Lang.ENGLISH
                val sourceSameAsPrimary = sourceLanguage == primaryLanguage
                val blankOrNotLatin = text.isBlank() || text.any(NON_LATIN_CONDITION)
                if (sourceSameAsPrimary || (!sourceIsEnglish && blankOrNotLatin)) {
                    Lang.ENGLISH
                } else {
                    primaryLanguage
                }
            }

            PRIMARY -> primaryLanguage
            LAST_USED -> states.lastLanguages.target
            else -> settings.targetLanguage
        }?.takeIf { isSupportedTargetLanguage(it) } ?: primaryLanguage
    }

    override fun updateLastLanguages(srcLang: Lang, targetLang: Lang) {
        states.lastLanguages = LanguagePair(srcLang, targetLang)
    }

    override fun translate(text: String, srcLang: Lang, targetLang: Lang) {
        val request = Presenter.Request(text, srcLang, targetLang, translateService.translator.id)
        if (text.isBlank() || request == currentRequest) {
            return
        }

        TextToSpeech.getInstance().stop()

        currentRequest = request
        if (recordHistory) {
            states.addHistory(text)
        }

        getCache(text, srcLang, targetLang)?.let { cache ->
            onPostResult(request) { showTranslation(request, cache, true) }
            return
        }

        view.showStartTranslate(request, text)

        translateService.translate(text, srcLang, targetLang, ResultListener(this, request))
    }

    private inline fun onPostResult(request: Presenter.Request, block: View.() -> Unit) {
        if (request == currentRequest && !view.disposed) {
            view.block()
            currentRequest = null
        }
    }

    private class ResultListener(presenter: TranslationPresenter, val request: Presenter.Request) : TranslateListener {

        private val presenterRef: WeakReference<TranslationPresenter> = WeakReference(presenter)

        override fun onSuccess(translation: Translation) {
            val presenter = presenterRef.get()
            if (presenter !== null) {
                presenter.onPostResult(request) { showTranslation(request, translation, false) }
            } else {
                LOGGER.w("We lost the presenter!")
            }
        }

        override fun onError(throwable: Throwable) {
            val presenter = presenterRef.get()
            if (presenter !== null) {
                presenter.onPostResult(request) { showError(request, throwable) }
            } else {
                LOGGER.w("We lost the presenter!")
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getInstance(TranslationPresenter::class.java)
    }
}
