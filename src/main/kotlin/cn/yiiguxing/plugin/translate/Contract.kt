/**
 * Contract
 * <p>
 * Created by Yii.Guxing on 2017-09-16 0016.
 */
package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import com.intellij.openapi.Disposable

data class SupportedLanguages(val source: List<Lang>, val target: List<Lang>)

interface Presenter {

    /**
     * 历史记录列表
     */
    val histories: List<String>

    /**
     * 主要语言
     */
    val primaryLanguage: Lang

    /**
     * 已支持的语言
     */
    val supportedLanguages: SupportedLanguages

    /**
     * @return 缓存
     */
    fun getCache(srcLang: Lang, targetLang: Lang, text: String): Translation?

    /**
     * 翻译
     */
    fun translate(srcLang: Lang, targetLang: Lang, text: String)
}

interface View : Disposable {

    val disposed: Boolean

    /**
     * 显示开始翻译
     *
     * @param  text 查询字符串
     */
    fun showStartTranslate(text: String)

    /**
     * 显示翻译结果
     */
    fun showTranslation(translation: Translation)

    /**
     * 显示错误信息
     *
     * @param errorMessage 错误信息
     */
    fun showError(errorMessage: String, throwable: Throwable)
}