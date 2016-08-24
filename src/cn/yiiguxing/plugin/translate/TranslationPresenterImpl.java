package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class TranslationPresenterImpl implements TranslationPresenter {
    private static final int HISTORY_SIZE = 50;
    private static final List<String> sHistory = new ArrayList<>(HISTORY_SIZE);

    private final TranslationView mTranslationView;

    private String currentQuery;

    public TranslationPresenterImpl(@NotNull TranslationView view) {
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

        List<String> history = TranslationPresenterImpl.sHistory;
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

        currentQuery = query;
        Translation.get().query(query, new Translation.Callback() {
            @Override
            public void onQuery(String query, QueryResult result) {
                onPostResult(query, result);
            }
        });
    }

    private void onPostResult(String query, QueryResult result) {
        if (Utils.isEmptyOrBlankString(query) || !query.equals(currentQuery))
            return;

        currentQuery = null;
        String errorMessage = Utils.getErrorMessage(result);
        if (errorMessage != null) {
            mTranslationView.showError(errorMessage);
        } else {
            mTranslationView.showResult(query, result);
        }
    }
}
