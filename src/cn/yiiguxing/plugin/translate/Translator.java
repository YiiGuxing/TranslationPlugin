package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.Future;

@SuppressWarnings("WeakerAccess")
public final class Translator {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String BASIC_URL = "http://fanyi.youdao.com/openapi.do";

    private static final String DEFAULT_API_KEY_NAME = "TranslationPlugin";
    private static final String DEFAULT_API_KEY_VALUE = "1473510108";

    private static final Logger LOGGER = Logger.getInstance("#" + Translator.class.getCanonicalName());

    private final LruCache<String, QueryResult> mCache = new LruCache<String, QueryResult>(200);
    private Future<?> mCurrentTask;

    private Translator() {
    }

    public static Translator getInstance() {
        return ServiceManager.getService(Translator.class);
    }

    @Nullable
    public QueryResult getCache(@NotNull String query) {
        synchronized (mCache) {
            return mCache.get(query);
        }
    }

    public void query(String query, Callback callback) {
        if (Utils.isEmptyOrBlankString(query)) {
            if (callback != null) {
                callback.onQuery(query, null);
            }

            return;
        }

        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
            mCurrentTask = null;
        }

        QueryResult cache;
        synchronized (mCache) {
            cache = mCache.get(query);
        }
        if (cache != null) {
            if (callback != null) {
                callback.onQuery(query, cache);
            }
        } else {
            mCurrentTask = ApplicationManager.getApplication().executeOnPooledThread(new QueryRequest(query, callback));
        }
    }

    static String getQueryUrl(String query) {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Settings settings = Settings.getInstance();

        String apiKeyName;
        String apiKeyValue;
        if (settings.isUseDefaultKey()) {
            apiKeyName = DEFAULT_API_KEY_NAME;
            apiKeyValue = DEFAULT_API_KEY_VALUE;
        } else {
            apiKeyName = settings.getApiKeyName();
            apiKeyValue = settings.getApiKeyValue();

            if (Utils.isEmptyOrBlankString(apiKeyName) || Utils.isEmptyOrBlankString(apiKeyValue)) {
                apiKeyName = DEFAULT_API_KEY_NAME;
                apiKeyValue = DEFAULT_API_KEY_VALUE;
            }
        }

        return BASIC_URL + "?type=data&doctype=json&version=1.1&keyfrom=" + apiKeyName + "&key=" +
                apiKeyValue + "&q=" + encodedQuery;
    }

    private final class QueryRequest implements Runnable {

        private final String mQuery;
        private final Callback mCallback;

        QueryRequest(String query, Callback callback) {
            mQuery = query;
            mCallback = callback;
        }

        @Override
        public void run() {
            final String query = mQuery;
            final String url = getQueryUrl(query);

            QueryResult result = null;
            try {
                String json = HttpRequests.request(url).readString(null);
                LOGGER.info(json);

                if (!Utils.isEmptyOrBlankString(json)) {
                    result = new Gson().fromJson(json, QueryResult.class);
                }
            } catch (IOException e) {
                LOGGER.warn(e);

                result = new QueryResult();
                result.setErrorCode(QueryResult.ERROR_CODE_FAIL);
                result.setMessage(e.getMessage());
            } catch (JsonSyntaxException e) {
                LOGGER.warn(e);

                result = new QueryResult();
                result.setErrorCode(QueryResult.ERROR_CODE_RESTRICTED);
            }

            if (result != null) {
                result.checkError();
                if (result.getErrorCode() == QueryResult.ERROR_CODE_NONE) {
                    synchronized (mCache) {
                        mCache.put(query, result);
                    }
                }
            }

            System.out.println("query: " + query);
            System.out.println("result: " + result);

            if (mCallback != null) {
                mCallback.onQuery(query, result);
            }
        }
    }

    public interface Callback {
        void onQuery(@Nullable String query, @Nullable QueryResult result);
    }

}
