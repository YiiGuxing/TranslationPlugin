package cn.yiiguxing.plugin.translate.ui

import javax.swing.text.*


/**
 * WarpEditorKit
 */
class WarpEditorKit : StyledEditorKit() {

    override fun getViewFactory(): ViewFactory = DEFAULT_FACTORY

    private class StyledViewFactory : ViewFactory {

        override fun create(elem: Element): View = when (elem.name) {
            AbstractDocument.ContentElementName -> WarpLabelView(elem)
            AbstractDocument.ParagraphElementName -> ParagraphView(elem)
            AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
            StyleConstants.ComponentElementName -> ComponentView(elem)
            StyleConstants.IconElementName -> IconView(elem)
            else -> LabelView(elem)
        }
    }

    private class WarpLabelView(elem: Element) : LabelView(elem) {

        override fun getMinimumSpan(axis: Int): Float {
            return when (axis) {
                View.X_AXIS -> 0f
                View.Y_AXIS -> super.getMinimumSpan(axis)
                else -> throw IllegalArgumentException("Invalid axis: $axis")
            }
        }
    }

    companion object {
        private val DEFAULT_FACTORY = StyledViewFactory()
    }
}