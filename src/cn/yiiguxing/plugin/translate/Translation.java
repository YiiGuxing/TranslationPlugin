package cn.yiiguxing.plugin.translate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.Future;

@SuppressWarnings("WeakerAccess")
public class Translation {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String BASIC_URL = "http://fanyi.youdao.com/openapi.do";

    private static final String DEFAULT_API_KEY_NAME = "TranslationPlugin";
    private static final String DEFAULT_API_KEY_VALUE = "1473510108";

    @SuppressWarnings("SpellCheckingInspection")
    private static final Logger LOG = Logger.getInstance("#cn.yiiguxing.plugin.translate.Translation");

    private static final Translation TRANSLATION = new Translation();

    private final LruCache<String, QueryResult> mCache = new LruCache<>(200);
    private Future<?> mCurrentTask;

    private Translation() {
    }

    public static Translation get() {
        return TRANSLATION;
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

        String apiKeyName;
        String apiKeyValue;
        boolean useDefaultKey = Settings.isUseDefaultKey();
        if (useDefaultKey) {
            apiKeyName = DEFAULT_API_KEY_NAME;
            apiKeyValue = DEFAULT_API_KEY_VALUE;
        } else {
            apiKeyName = Settings.getApiKeyName();
            apiKeyValue = Settings.getApiKeyValue();

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
            CloseableHttpClient httpClient = HttpClients.createDefault();

            QueryResult result;
            try {
                String url = getQueryUrl(query);
                HttpGet httpGet = new HttpGet(url);
                result = httpClient.execute(httpGet, new YouDaoResponseHandler());
                if (result != null && result.getErrorCode() == QueryResult.ERROR_CODE_NONE) {
                    synchronized (mCache) {
                        mCache.put(query, result);
                    }
                }
            } catch (Exception e) {
                LOG.warn("query...", e);
                result = null;
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("query: " + query);
            System.out.println("result: " + result);

            final QueryResult postResult = result;
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onQuery(query, postResult);
                    }
                }
            });
        }
    }

    private final class YouDaoResponseHandler implements ResponseHandler<QueryResult> {

        @Override
        public QueryResult handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if (entity == null)
                    return null;

                String json = EntityUtils.toString(entity);
                LOG.info(json);

                try {
                    return new Gson().fromJson(json, QueryResult.class);
                } catch (JsonSyntaxException e) {
                    LOG.warn(e);

                    QueryResult result = new QueryResult();
                    result.setErrorCode(QueryResult.ERROR_CODE_RESTRICTED);

                    return result;
                }
            } else {
                String message = "Unexpected response status: " + status;
                LOG.warn(message);
                throw new ClientProtocolException(message);
            }
        }
    }

    public interface Callback {
        void onQuery(String query, QueryResult result);
    }

}
