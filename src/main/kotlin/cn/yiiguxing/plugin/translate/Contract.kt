/**
 * Contract
 * <p>
 * Created by Yii.Guxing on 2017-09-16 0016.
 */
package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.model.QueryResult

interface Presenter {
    /**
     * 历史记录列表
     */
    val histories: List<String>

    /**
     * @param query 查询
     * @return 缓存
     */
    fun getCache(query: String): QueryResult?

    /**
     * 查询翻译
     *
     * @param query 查询字符串
     */
    fun query(query: String)
}

interface View {
    /**
     * 显示翻译结果
     *
     * @param query  查询字符串
     * @param result 翻译结果
     */
    fun showResult(query: String, result: QueryResult)

    /**
     * 显示错误信息
     *
     * @param query 查询字符串
     * @param error 错误信息
     */
    fun showError(query: String, error: String)
}