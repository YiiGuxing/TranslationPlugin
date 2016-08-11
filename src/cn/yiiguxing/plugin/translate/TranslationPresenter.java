package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class TranslationPresenter {

    private static final int HISTORY_SIZE = 50;
    private static final List<String> sHistory = new ArrayList<>(HISTORY_SIZE);

    private final TranslationView mTranslationView;

    private String currentQuery;

    public TranslationPresenter(@NotNull TranslationView view) {
        this.mTranslationView = Objects.requireNonNull(view, "view cannot be null.");
    }

    @NotNull
    public List<String> getHistory() {
        return Collections.unmodifiableList(sHistory);
    }

    public void query(@Nullable String query) {
        if (Utils.isEmptyOrBlankString(query) || query.equals(currentQuery))
            return;

        query = query.trim();

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

        currentQuery = query;
        Translation.get().query(query, new QueryCallback(this));
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

    private static class QueryCallback implements Translation.Callback {
        private final Reference<TranslationPresenter> presenterReference;

        private QueryCallback(TranslationPresenter presenter) {
            this.presenterReference = new WeakReference<>(presenter);
        }

        @Override
        public void onQuery(String query, QueryResult result) {
            TranslationPresenter presenter = presenterReference.get();
            if (presenter != null) {
                presenter.onPostResult(query, result);
            }
        }
    }

}
