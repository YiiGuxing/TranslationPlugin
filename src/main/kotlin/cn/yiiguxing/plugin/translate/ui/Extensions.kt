/*
 * Extensions
 * 
 * Created by Yii.Guxing on 2017/11/20
 */
package cn.yiiguxing.plugin.translate.ui

import javax.swing.JComboBox


/**
 * 当前选中项
 */
inline var <reified E> JComboBox<E>.selected: E?
    get() = selectedItem as? E
    set(value) {
        selectedItem = value
    }