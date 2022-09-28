package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.util.trimToSize
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.CollectionBean
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Transient
import java.util.*
import kotlin.properties.Delegates

/**
 * Persistent plugin states.
 */
@State(name = "Translation.States", storages = [(Storage(TranslationStorages.PREFERENCES_STORAGE_NAME))])
class TranslationStates : PersistentStateComponent<TranslationStates> {

    @CollectionBean
    private val histories: MutableList<String> = ArrayList(DEFAULT_HISTORY_SIZE)

    @MapAnnotation
    private val languageScores: MutableMap<Lang, Int> = EnumMap(Lang::class.java)

    val lastLanguages: LanguagePair = LanguagePair()

    var lastReplacementTargetLanguage: Lang? = null

    var pinTranslationDialog: Boolean = false
    var newTranslationDialogX: Int? = null
    var newTranslationDialogY: Int? = null
    var newTranslationDialogWidth: Int = 600
    var newTranslationDialogHeight: Int = 250
    var newTranslationDialogCollapseDictViewer = true

    /**
     * 最大历史记录长度
     */
    var maxHistorySize by Delegates.vetoable(DEFAULT_HISTORY_SIZE) { _, oldValue: Int, newValue: Int ->
        if (oldValue == newValue || newValue < 0) {
            return@vetoable false
        }

        trimHistoriesSize(newValue)
        true
    }

    @Transient
    private val dataChangePublisher: HistoriesChangedListener =
        ApplicationManager.getApplication().messageBus.syncPublisher(HistoriesChangedListener.TOPIC)

    override fun getState(): TranslationStates = this

    override fun loadState(state: TranslationStates) {
        XmlSerializerUtil.copyBean(state, this)
    }

    /**
     * @return 语言常用评分
     */
    fun getLanguageScore(lang: Lang): Int = languageScores[lang] ?: 0

    /**
     * 设置语言常用评分
     */
    fun setLanguageScore(lang: Lang, score: Int) {
        languageScores[lang] = score
    }

    /**
     * 增加语言常用评分
     */
    fun accumulateLanguageScore(lang: Lang) {
        if (lang != Lang.AUTO) {
            languageScores[lang] = languageScores.getOrDefault(lang, 0) + 1
        }
    }

    private fun trimHistoriesSize(maxSize: Int) {
        if (histories.trimToSize(maxSize)) {
            dataChangePublisher.onHistoriesChanged()
        }
    }

    /**
     * @return 历史上记录列表
     */
    fun getHistories(): List<String> = histories

    /**
     * 添加历史记录
     *
     * @param query 查询
     */
    fun addHistory(query: String) {
        val maxSize = maxHistorySize
        if (maxSize <= 0) {
            return
        }

        histories.run {
            val index = indexOf(query)
            if (index != 0) {
                if (index > 0) {
                    removeAt(index)
                }

                add(0, query)
                trimToSize(maxSize)
                dataChangePublisher.onHistoryItemChanged(query)
            }
        }
    }

    /**
     * 清除历史启启记录
     */
    fun clearHistories() {
        if (histories.isNotEmpty()) {
            histories.clear()
            dataChangePublisher.onHistoriesChanged()
        }
    }

    companion object {
        private const val DEFAULT_HISTORY_SIZE = 50

        /**
         * Get the instance of this service.
         *
         * @return the unique [TranslationStates] instance.
         */
        val instance: TranslationStates
            get() = ApplicationManager.getApplication().getService(TranslationStates::class.java)
    }

}

interface HistoriesChangedListener {

    fun onHistoriesChanged()

    fun onHistoryItemChanged(newHistory: String)

    companion object {
        @Topic.AppLevel
        val TOPIC: Topic<HistoriesChangedListener> =
            Topic.create("TranslateHistoriesChanged", HistoriesChangedListener::class.java)
    }
}
