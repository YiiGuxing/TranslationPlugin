package cn.yiiguxing.plugin.translate.action;

/**
 * 自动从最大范围内取词，优先选择
 */
public class EditorPopupMenuAction extends TranslateAction {

    public EditorPopupMenuAction() {
        super(AutoSelectionMode.INCLUSIVE, true);
    }

}
