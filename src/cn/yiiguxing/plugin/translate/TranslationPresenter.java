package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TranslationPresenter implements TranslationContract.Presenter {
    private static final int HISTORY_SIZE = 50;
    private static final List<String> sHistory = new ArrayList<>(HISTORY_SIZE);

    private final TranslationContract.View mTranslationView;

    private String currentQuery;

    public TranslationPresenter(@NotNull TranslationContract.View view) {
        this.mTranslationView = Objects.requireNonNull(view, "view cannot be null.");
    }

    @NotNull
    @Override
    public List<String> getHistory() {
        return Collections.unmodifiableList(sHistory);
    }

    @Override
    public void query(@Nullable String query) {
        if (Utils.isEmptyOrBlankString(query) || query.equals(currentQuery))
            return;

        query = query.trim();
        updateHistory(query);

        currentQuery = query;

        // 防止内存泄漏
        final Reference<TranslationPresenter> presenterRef = new WeakReference<>(this);
        Translator.getInstance().query(query, new Translator.Callback() {
            @Override
            public void onQuery(String query, QueryResult result) {
                TranslationPresenter presenter = presenterRef.get();
                if (presenter != null) {
                    presenter.onPostResult(query, result);
                }
            }
        });
    }

    private void updateHistory(String query) {
        List<String> history = TranslationPresenter.sHistory;
        int index = history.indexOf(query);
        if (index != 0) {
            if (index > 0) {
                history.remove(index);
            }
            if (history.size() >= HISTORY_SIZE) {
                history.remove(HISTORY_SIZE - 1);
            }

            history.add(0, query);
            mTranslationView.updateHistory();
        }
    }

    private void onPostResult(String query, QueryResult result) {
        if (Utils.isEmptyOrBlankString(query) || !query.equals(currentQuery))
            return;

        currentQuery = null;
        String errorMessage = Utils.getErrorMessage(result);
        if (errorMessage != null) {
            mTranslationView.showError(query, errorMessage);
        } else {
            mTranslationView.showResult(query, result);
        }
    }
}
