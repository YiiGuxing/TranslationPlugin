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

/**
 * 翻译器
 */
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
    public QueryResult getCache(@NotNull String query) {
        synchronized (mCache) {
            return mCache.get(query);
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
				/**
         * 2017-6-9 yangFan
         * 改变返回逻辑，以达新、老用户都能用，老用户不用重新申请KEY的目的
         */
        if (isOldValue(apiKeyValue)) {
            return BASIC_URL + "?type=data&doctype=json&version=1.1&keyfrom=" + apiKeyName + "&key=" +
                    apiKeyValue + "&q=" + encodedQuery;
        } else {
            return TranslatorURLFix.getFixedQueryUrl(apiKeyName, apiKeyValue, query);
        }

//        return BASIC_URL + "?type=data&doctype=json&version=1.1&keyfrom=" + apiKeyName + "&key=" +
//                apiKeyValue + "&q=" + encodedQuery;
    }

    /**
     * 2017-6-9 yangFan
     * 判断是否是有道翻译旧接口的API_KEY_VALUE
     * 依据1、旧接口value长度在10左右，新接口value长度在32左右
     * 依据2、旧接口value是纯数字，新接口value是大小写字母与数字的混合
     */
    public static boolean isOldValue(String str) {

        if (str.length() < 20) {
            return true;//长度小于20的是OldValue，返回true
        }
        for (int i = 0; i < str.length(); i++) {
            System.out.println(str.charAt(i));
            if (!Character.isDigit(str.charAt(i))) {
                return false;//非纯数字是newValue，返回false
            }
        }
        return true;//纯数字是OldValue，返回true
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
