package cn.yiiguxing.plugin.translate;

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
import java.net.URLEncoder;
import java.util.concurrent.Future;

public class Translate {

    @SuppressWarnings("all")
    private static final String BASIC_URL = "http://fanyi.youdao.com/openapi.do?keyfrom=Tinkling&key=1977836024&type=data&doctype=json&version=1.1&q=";
    @SuppressWarnings("all")
    private static final Logger LOG = Logger.getInstance("#cn.yiiguxing.plugin.translate.Translate");

    private static final Translate TRANSLATE = new Translate();

    private final LruCache<String, QueryResult> mCache = new LruCache<>(200);
    private Future<?> mCurrentTask;

    private Translate() {
    }

    public static Translate get() {
        return TRANSLATE;
    }

    public void search(String query, Callback callback) {
        if (query == null || query.trim().length() == 0)
            return;

        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
            mCurrentTask = null;
        }

        QueryResult cache = mCache.get(query);
        if (cache != null) {
            if (callback != null) {
                callback.onQuery(query, cache);
            }
        } else {
            mCurrentTask = ApplicationManager.getApplication().executeOnPooledThread(new QueryRequest(query, callback));
        }
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
                HttpGet httpGet = new HttpGet(BASIC_URL + URLEncoder.encode(query, "UTF-8"));
                result = httpClient.execute(httpGet, new YouDaoResponseHandler());
                if (result != null && result.getErrorCode() != QueryResult.ERROR_CODE_FAIL) {
                    mCache.put(query, result);
                }
            } catch (Exception e) {
                LOG.error("query...", e);
                result = null;
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LOG.info("query: " + query);
            LOG.info("result: " + result);

            final QueryResult postResult = result;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (mCallback != null) {
                    mCallback.onQuery(query, postResult);
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
                LOG.debug(json);

                return JsonHelper.getYouDaoResultEntity(json);
            } else {
                String message = "Unexpected response status: " + status;
                LOG.error(message);
                throw new ClientProtocolException(message);
            }
        }

    }

    public interface Callback {
        void onQuery(String query, QueryResult result);
    }

}
