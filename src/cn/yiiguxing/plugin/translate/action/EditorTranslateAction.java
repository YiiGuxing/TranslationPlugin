package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Settings;
import org.jetbrains.annotations.NotNull;

/**
 * 自动从最大范围内取词，优先选择
 */
public class EditorTranslateAction extends TranslateAction {

    private final Settings settings;

    public EditorTranslateAction() {
        super(true);
        settings = Settings.getInstance();
    }

    @NotNull
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return settings.getAutoSelectionMode();
    }
}
