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
val <E> JComboBox<E>.selected: E get() = getItemAt(selectedIndex)