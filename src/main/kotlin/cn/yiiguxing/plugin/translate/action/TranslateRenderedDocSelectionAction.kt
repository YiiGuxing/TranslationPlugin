package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonImpl
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.PositionTracker
import icons.TranslationIcons
import java.awt.Point
import java.lang.reflect.Method
import javax.swing.JEditorPane

class TranslateRenderedDocSelectionAction : UpdateInBackgroundCompatAction(), ImportantTranslationAction, PopupAction, DumbAware {

    private val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

    init {
        templatePresentation.icon = TranslationIcons.Translation
        templatePresentation.text = adaptedMessage("action.TranslateRenderedDocSelectionAction.text")

        // 为了在菜单项上显示快捷键提示
        ActionManager.getInstance()
            .getAction(EditorTranslateAction.ACTION_ID)
            ?.let { copyShortcutFrom(it) }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.editor
        if (editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val editorPane = getPaneWithSelection(null, editor) as? JEditorPane
        e.presentation.isEnabledAndVisible = !editor.selectionModel.hasSelection(true) &&
                editorPane != null && !editorPane.selectedText.isNullOrBlank()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor ?: return
        val editorPane = getPaneWithSelection(null, editor) as? JEditorPane
        val selectedText = editorPane?.selectedText?.processBeforeTranslate()
        if (selectedText.isNullOrBlank()) {
            return
        }

        val positionInEditor = getSelectionPositionInEditor(editorPane) as? Point ?: return
        val scrollingModel = editor.scrollingModel
        if (!scrollingModel.visibleAreaOnScrollingFinished.contains(positionInEditor)) {
            scrollingModel.scrollTo(editor.xyToLogicalPosition(positionInEditor), ScrollType.MAKE_VISIBLE)
        }

        val positionTracker = MyPositionTracker(editor, editorPane)
        TranslationUIManager.showBalloon(editor, selectedText, positionTracker)
    }

    private class MyPositionTracker(
        private val editor: Editor,
        private val editorPane: JEditorPane
    ) : PositionTracker<Balloon>(editor.contentComponent) {

        private var lastPosition: RelativePoint? = null


        override fun recalculateLocation(balloon: Balloon): RelativePoint? {
            if (balloon.isDisposed) {
                return lastPosition
            }

            val positionStartInEditor = getSelectionPositionInEditor(editorPane) as Point? ?: return lastPosition
            val positionStartInPane = editorPane.modelToView2D(editorPane.selectionStart)
            val positionEndInPane = editorPane.modelToView2D(editorPane.selectionEnd)
            val positionEndXInEditor = positionEndInPane.x + positionStartInEditor.x - positionStartInPane.x
            val positionEndYInEditor = positionEndInPane.y + positionStartInEditor.y - positionStartInPane.y
            val lineHeight = editorPane.getFontMetrics(editorPane.font).height
            val x = minOf(positionEndXInEditor, (positionStartInEditor.x + positionEndXInEditor) / 2)
            val y = positionEndYInEditor + lineHeight

            val visibleArea = editor.scrollingModel.visibleArea
            val isInVisibleArea = visibleArea.contains(x, y)
            (balloon as? BalloonImpl)?.setLostPointer(!isInVisibleArea)

            return if (isInVisibleArea) {
                RelativePoint(editor.contentComponent, Point(x.toInt(), y.toInt()))
            } else {
                lastPosition ?: RelativePoint(
                    editor.contentComponent,
                    with(visibleArea) { Point((x + width / 3).toInt(), (y + height / 2).toInt()) })
            }.also { lastPosition = it }
        }
    }

    companion object {
        private val getPaneWithSelection: Method? by lazy {
            try {
                val clazz = Class.forName("com.intellij.codeInsight.documentation.render.DocRenderSelectionManager")
                clazz.getDeclaredMethod("getPaneWithSelection", Editor::class.java)
            } catch (e: Throwable) {
                null
            }
        }

        private val getSelectionPositionInEditor: Method? by lazy {
            try {
                val clazz = Class.forName("com.intellij.codeInsight.documentation.render.DocRenderer\$EditorPane")
                clazz.getDeclaredMethod("getSelectionPositionInEditor")
            } catch (e: Throwable) {
                null
            }
        }

        private operator fun Method?.invoke(obj: Any?, vararg args: Any?): Any? {
            if (this == null) return null

            isAccessible = true
            try {
                return invoke(obj, *args)
            } finally {
                isAccessible = false
            }
        }
    }
}