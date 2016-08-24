package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public interface TranslationPresenter {

    @NotNull
    List<String> getHistory();

    void query(@Nullable String query);

}
