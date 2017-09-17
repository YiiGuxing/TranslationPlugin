package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import cn.yiiguxing.plugin.translate.util.LruCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

/**
 * 翻译器
 */
@SuppressWarnings("WeakerAccess")
public final class Translator {

    private static final Logger LOGGER = Logger.getInstance("#" + Translator.class.getCanonicalName());

    private final Settings mSettings = Settings.Companion.getInstance();
    private final LruCache<CacheKey, QueryResult> mCache = new LruCache<CacheKey, QueryResult>(500);
    private Future<?> mCurrentTask;

    private Translator() {
    }

    /**
     * @return {@link Translator} 的实例
     */
    public static Translator getInstance() {
        return ServiceManager.getService(Translator.class);
    }

    /**
     * 获取缓存
     */
    @Nullable
    public QueryResult getCache(@NotNull CacheKey key) {
        synchronized (mCache) {
            return mCache.get(key);
        }
    }

    /**
     * 查询翻译
     *
     * @param query    目标字符串
     * @param callback 回调
     */
    public void query(String query, Callback callback) {
        if (Utils.isEmptyOrBlankString(query)) {
            if (callback != null) {
                callback.onQuery(query, null);
            }

            return;
        }

        if (mCurrentTask != null) {
            mCurrentTask.cancel(false);
            mCurrentTask = null;
        }

        final Lang langFrom = Utils.notNull(mSettings.getLangFrom(), Lang.AUTO);
        final Lang langTo = Utils.notNull(mSettings.getLangTo(), Lang.AUTO);

        QueryResult cache;
        synchronized (mCache) {
            cache = mCache.get(new CacheKey(langFrom, langTo, query));
        }
        if (cache != null) {
            if (callback != null) {
                callback.onQuery(query, cache);
            }
        } else {
            mCurrentTask = ApplicationManager
                    .getApplication()
                    .executeOnPooledThread(new QueryRequest(langFrom, langTo, query, callback));
        }
    }

    private String getQueryUrl(@NotNull Lang langFrom, @NotNull Lang langTo, @NotNull String query) {
        Settings settings = mSettings;

        String appId = settings.getAppId();
        String privateKey = settings.getAppPrivateKey();

        String salt = String.valueOf(System.currentTimeMillis());
        String sign = Utils.md5(appId + query + salt + privateKey);

        return ConstantsKt.YOUDAO_TRANSLATE_URL +
                "?appKey=" + Utils.urlEncode(appId) +
                "&from=" + langFrom.getCode() +
                "&to=" + langTo.getCode() +
                "&salt=" + salt +
                "&sign=" + sign +
                "&q=" + Utils.urlEncode(query);
    }

    private final class QueryRequest implements Runnable {

        private final Lang mLangFrom;
        private final Lang mLangTo;
        private final String mQuery;
        private final Callback mCallback;

        QueryRequest(@NotNull Lang langFrom, @NotNull Lang langTo, @NotNull String query, Callback callback) {
            mLangFrom = langFrom;
            mLangTo = langTo;
            mQuery = query;
            mCallback = callback;
        }

        @Override
        public void run() {
            final String query = mQuery;

            QueryResult result = null;
            try {
                final String url = getQueryUrl(mLangFrom, mLangTo, query);
                LOGGER.info("query url: " + url);

                String json = HttpRequests.request(url).readString(null);
                LOGGER.info(json);

                if (!Utils.isEmptyOrBlankString(json)) {
                    result = new Gson().fromJson(json, QueryResult.class);
                }
            } catch (JsonSyntaxException e) {
                LOGGER.warn(e);

                result = new QueryResult();
                result.setErrorCode(QueryResult.CODE_JSON_SYNTAX_ERROR);
            } catch (Exception e) {
                LOGGER.warn(e);

                result = new QueryResult();
                result.setErrorCode(QueryResult.CODE_ERROR);
                result.setMessage(e.getMessage());
            }

            if (result == null) {
                result = new QueryResult();
                result.setErrorCode(QueryResult.CODE_ERROR);
            }

            result.checkError();
            if (result.isSuccessful()) {
                synchronized (mCache) {
                    mCache.put(new CacheKey(mLangFrom, mLangTo, query), result);
                }
            }

            System.out.println("query: " + query);
            System.out.println("result: " + result);

            if (mCallback != null) {
                mCallback.onQuery(query, result);
            }
        }
    }

    /**
     * 翻译回调接口
     */
    public interface Callback {
        /**
         * 翻译结束后的回调方法
         *
         * @param query  查询字符串
         * @param result 翻译结果
         */
        void onQuery(@Nullable String query, @Nullable QueryResult result);
    }

}
