package cn.yiiguxing.plugin.translate.action;

/**
 * 自动取单个词，忽略选择
 */
public class ExclusiveTranslateAction extends TranslateAction {

    public ExclusiveTranslateAction() {
        super(AutoSelectionMode.EXCLUSIVE, false);
    }

}
