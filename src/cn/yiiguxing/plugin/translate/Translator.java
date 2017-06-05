package cn.yiiguxing.plugin.translate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cn.yiiguxing.plugin.translate.model.QueryResult;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Future;

/**
 * 翻译器
 */
@SuppressWarnings("WeakerAccess")
public final class Translator {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String BASIC_URL = "https://openapi.youdao.com/api";

    // 新版的应用ID  appKey
    private static final String DEFAULT_API_KEY_NAME = "22e0c7b47735dc48";

    // 新版的应用密钥
    private static final String DEFAULT_API_KEY_VALUE = "EypH6vWGXE2tUUfyNy2liNBDpicRECWW";

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
//        try {
//            query = new String(query.getBytes(), "UTF-8");
//        } catch (UnsupportedEncodingException ignore) {
//
//        }

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

        String salt = "1";
        // 签名，通过md5(appKey+q+salt+密钥)生成
        final String sign = md5(apiKeyName + query + salt + apiKeyValue);
        // 0B532BC7804652685ECDA97D22EBEF25

        // http://openapi.youdao.com/api?q=good&from=en&to=zh_CHS&appKey=ff889495-4b45-46d9-8f48-946554334f2a&salt=2&sign=1995882C5064805BC30A39829B779D7B

        final String url = BASIC_URL + "?" + getTranslateMethod(query) +
                "&appKey=" + apiKeyName +
                "&salt=" + salt +
                "&sign=" + sign +
                "&q=" + encode(query);

//        System.out.println("request url : " + url);

        return url;
    }

    /**
     * 通过传入的字符串判断是英译汉还是汉译英
     */
    private static String getTranslateMethod(final String query) {
        if (query.matches("[a-zA-Z ]+")) {
            return "from=en&to=zh_CHS";
        }
        return "from=zh_CHS&to=en";
    }

    /**
     * 生成32位MD5摘要
     */
    public static String md5(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            byte[] btInput = string.getBytes("utf-8");
            /** 获得MD5摘要算法的 MessageDigest 对象 */
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            /** 使用指定的字节更新摘要 */
            mdInst.update(btInput);
            /** 获得密文 */
            byte[] md = mdInst.digest();
            /** 把密文转换成十六进制的字符串形式 */
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return null;
        }
    }


    /**
     * 进行URL编码
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException ignore) {

        }

        return input;
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
