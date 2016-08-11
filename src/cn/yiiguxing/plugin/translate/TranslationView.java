package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;

public interface TranslationView {

    void updateHistory();

    void showResult(@NotNull String query, @NotNull QueryResult result);

    void showError(@NotNull String error);

}
