/**
 * Contract
 * <p>
 * Created by Yii.Guxing on 2017-09-16 0016.
 */
package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Translation
import com.intellij.openapi.Disposable

interface Presenter {
    /**
     * 历史记录列表
     */
    val histories: List<String>

    /**
     * @return 缓存
     */
    fun getCache(text: String): Translation?

    /**
     * 翻译
     */
    fun translate(text: String)
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
    fun showTranslation(text: String, translation: Translation)

    /**
     * 显示错误信息
     *
     * @param text 翻译内容
     * @param errorMessage 错误信息
     */
    fun showError(text: String, errorMessage: String)
}