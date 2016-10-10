package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TranslationContract {

    interface Presenter {
        @NotNull
        List<String> getHistory();

        @Nullable
        QueryResult getCache(String query);

        void query(@Nullable String query);
    }

    interface View {
        void updateHistory();

        void showResult(@NotNull String query, @NotNull QueryResult result);

        void showError(@NotNull String query, @NotNull String error);
    }

}
