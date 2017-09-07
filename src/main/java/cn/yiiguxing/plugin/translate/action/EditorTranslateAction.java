package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Settings;
import org.jetbrains.annotations.NotNull;

/**
 * 翻译动作，自动从最大范围内取词，优先选择
 */
public class EditorTranslateAction extends TranslateAction {

    private final Settings mSettings;

    public EditorTranslateAction() {
        super(true);
        mSettings = Settings.getInstance();
    }

    @NotNull
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return mSettings.getAutoSelectionMode();
    }
}
