package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

public class TranslationPresenter implements TranslationContract.Presenter {

    private final AppStorage mAppStorage;

    private final Translator mTranslator;
    private final TranslationContract.View mTranslationView;

    private String mCurrentQuery;

    public TranslationPresenter(@NotNull TranslationContract.View view) {
        mTranslator = Translator.getInstance();
        this.mTranslationView = view;
        mAppStorage = AppStorage.getInstance();
    }

    @NotNull
    @Override
    public List<String> getHistory() {
        return mAppStorage.getHistories();
    }

    @Nullable
    @Override
    public QueryResult getCache(String query) {
        if (Utils.isEmptyOrBlankString(query))
            return null;

        return mTranslator.getCache(query);
    }

    @Override
    public void query(@Nullable String query) {
        if (Utils.isEmptyOrBlankString(query) || query.equals(mCurrentQuery))
            return;

        query = query.trim();
        mAppStorage.addHistory(query);

        mCurrentQuery = query;

        // 防止内存泄漏
        final Reference<TranslationPresenter> presenterRef = new WeakReference<TranslationPresenter>(this);
        mTranslator.query(query, new Translator.Callback() {
            @Override
            public void onQuery(final String query, final QueryResult result) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        TranslationPresenter presenter = presenterRef.get();
                        if (presenter != null) {
                            presenter.onPostResult(query, result);
                        }
                    }
                }, ModalityState.any());
            }
        });
    }

    private void onPostResult(String query, QueryResult result) {
        if (Utils.isEmptyOrBlankString(query) || !query.equals(mCurrentQuery))
            return;

        mCurrentQuery = null;
        String errorMessage = Utils.getErrorMessage(result);
        if (errorMessage != null) {
            mTranslationView.showError(query, errorMessage);
        } else {
            mTranslationView.showResult(query, result);
        }
    }
}
