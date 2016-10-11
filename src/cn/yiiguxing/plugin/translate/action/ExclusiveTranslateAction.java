package cn.yiiguxing.plugin.translate.action;

import org.jetbrains.annotations.NotNull;

/**
 * 自动取单个词，忽略选择
 */
public class ExclusiveTranslateAction extends TranslateAction {

    public ExclusiveTranslateAction() {
        super(false);
    }

    @NotNull
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return AutoSelectionMode.EXCLUSIVE;
    }
}
