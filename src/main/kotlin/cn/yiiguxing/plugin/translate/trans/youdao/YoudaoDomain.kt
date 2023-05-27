@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.BUNDLE
import cn.yiiguxing.plugin.translate.message
import org.jetbrains.annotations.PropertyKey

/**
 * 领域模型
 */
enum class YoudaoDomain(
    val value: String,
    @PropertyKey(resourceBundle = BUNDLE)
    displayNamePropertyKey: String
) {

    /** 通用领域 */
    GENERAL("general", "youdao.domain.general"),

    /** 计算机领域 */
    COMPUTERS("computers", "youdao.domain.model.computers"),

    /** 金融经济领域 */
    FINANCE("finance", "youdao.domain.finance"),

    /** 医学领域 */
    MEDICINE("medicine", "youdao.domain.medicine");

    val displayName: String by lazy { message(displayNamePropertyKey) }

}