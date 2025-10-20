package cn.yiiguxing.plugin.translate.documentation.actions

import cn.yiiguxing.plugin.translate.action.ACTION_HIGH_PRIORITY
import cn.yiiguxing.plugin.translate.action.EditorTranslateAction
import cn.yiiguxing.plugin.translate.action.ImportantTranslationAction
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonImpl
import cn.yiiguxing.plugin.translate.util.processBeforeTranslate
import com.intellij.openapi.actionSystem.*
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

internal class TranslateRenderedDocSelectionAction : AnAction(), ImportantTranslationAction, PopupAction, DumbAware {

    private val AnActionEvent.editor: Editor? get() = CommonDataKeys.EDITOR.getData(dataContext)

    override val priority: Int = ACTION_HIGH_PRIORITY

    init {
        templatePresentation.icon = TranslationIcons.Translation
        templatePresentation.text = adaptedMessage("action.TranslateRenderedDocSelectionAction.text")
        templatePresentation.description = adaptedMessage("action.TranslateRenderedDocSelectionAction.description")

        ActionManager.getInstance()
            .getAction(EditorTranslateAction.ACTION_ID)
            ?.let { copyShortcutFrom(it) }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.editor
        if (editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.isEnabledAndVisible = !editor.selectionModel.hasSelection(true)
                && !editor.getInlinePaneWithSelection()?.selectedText.isNullOrBlank()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.editor ?: return
        val editorPane = editor.getInlinePaneWithSelection() ?: return
        val selectedText = editorPane.selectedText?.processBeforeTranslate()
        if (selectedText.isNullOrBlank()) {
            return
        }

        val positionInEditor = editorPane.getSelectionPositionInEditor() ?: return
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

            val positionStartInEditor = editorPane.getSelectionPositionInEditor() ?: return lastPosition
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

    @Suppress("CompanionObjectInExtension")
    companion object {
        private val getPaneWithSelectionMethod: Method? by lazy {
            try {
                val clazz = Class.forName("com.intellij.codeInsight.documentation.render.DocRenderSelectionManager")
                clazz.getDeclaredMethod("getPaneWithSelection", Editor::class.java)
            } catch (_: Throwable) {
                null
            }
        }

        private fun Editor.getInlinePaneWithSelection(): JEditorPane? {
            return getPaneWithSelectionMethod(null, this) as? JEditorPane
        }

        private var selectionPositionMethod: Method? = null
        private var isSelectionPositionMethodInitialized = false

        @Synchronized
        private fun getSelectionPositionMethod(obj: Any): Method? {
            if (!isSelectionPositionMethodInitialized) {
                selectionPositionMethod = try {
                    obj.javaClass.getDeclaredMethod("getSelectionPositionInEditor")
                } catch (_: Throwable) {
                    null
                } finally {
                    isSelectionPositionMethodInitialized = true
                }
            }

            return selectionPositionMethod
        }

        private fun JEditorPane.getSelectionPositionInEditor(): Point? {
            return getSelectionPositionMethod(this)(this) as? Point
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