package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor


interface ImportantTranslationAction


val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

fun hasEditorSelection(e: AnActionEvent): Boolean = e.editor?.selectionModel?.hasSelection() ?: false

fun mayTranslateWithNoSelection(e: AnActionEvent): Boolean {
    val isContextMenu = e.place == ActionPlaces.EDITOR_POPUP
    val hideWithNoSelection = isContextMenu && Settings.showActionsInContextMenuOnlyWithSelection

    return !hideWithNoSelection
}

fun showReplacementActionInContextMenu(e: AnActionEvent): Boolean {
    val isContextMenu = e.place == ActionPlaces.EDITOR_POPUP
    return !isContextMenu || Settings.showReplacementAction
}