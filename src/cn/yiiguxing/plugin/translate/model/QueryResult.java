package cn.yiiguxing.plugin.translate.model;

import cn.yiiguxing.plugin.translate.Constants;
import cn.yiiguxing.plugin.translate.Utils;
import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class QueryResult {

    public static final int CODE_ERROR = -1;
    public static final int CODE_JSON_SYNTAX_ERROR = -2;

    private static final Map<Integer, String> ERROR_MAP = new HashMap<Integer, String>();

    static {
        ERROR_MAP.put(101, "缺少必填的参数");
        ERROR_MAP.put(102, "不支持的语言类型");
        ERROR_MAP.put(103, "翻译文本过长");
        ERROR_MAP.put(104, "不支持的API类型");
        ERROR_MAP.put(105, "不支持的签名类型");
        ERROR_MAP.put(106, "不支持的响应类型");
        ERROR_MAP.put(107, "不支持的传输加密类型");
        ERROR_MAP.put(108, "AppKey无效 - " + Constants.LINK_SETTINGS);
        ERROR_MAP.put(109, "BatchLog格式不正确");
        ERROR_MAP.put(110, "无相关服务的有效实例");
        ERROR_MAP.put(111, "账号无效或者账号已欠费 - " + Constants.LINK_SETTINGS);
        ERROR_MAP.put(201, "解密失败");
        ERROR_MAP.put(202, "签名检验失败");
        ERROR_MAP.put(203, "访问IP地址不在可访问IP列表");
        ERROR_MAP.put(301, "辞典查询失败");
        ERROR_MAP.put(302, "翻译查询失败");
        ERROR_MAP.put(303, "服务器异常");
        ERROR_MAP.put(401, "账户已经欠费");
    }

    @SerializedName("query")
    private String query;
    @SerializedName("errorCode")
    private int errorCode;
    @SerializedName("translation")
    private String[] translation;

    private String message;

    @SerializedName("basic")
    private BasicExplain basicExplain;
    @SerializedName("web")
    private WebExplain[] webExplains;

    public QueryResult() {
    }

    public QueryResult(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getTranslation() {
        return translation;
    }

    public void setTranslation(String[] translation) {
        this.translation = translation;
    }

    public BasicExplain getBasicExplain() {
        return basicExplain;
    }

    public void setBasicExplain(BasicExplain basicExplain) {
        this.basicExplain = basicExplain;
    }

    public WebExplain[] getWebExplains() {
        return webExplains;
    }

    public void setWebExplains(WebExplain[] webExplains) {
        this.webExplains = webExplains;
    }

    public void checkError() {
        if (isSuccessful() && (translation == null || translation.length == 0) &&
                (basicExplain == null || basicExplain.getExplains() == null || basicExplain.getExplains().length == 0))
            errorCode = 302;

        if (!isSuccessful() && Utils.isEmptyOrBlankString(message)) {
            String msg = ERROR_MAP.get(errorCode);
            message = MoreObjects.firstNonNull(msg, String.format("未知错误:[%d]", errorCode));
        }
    }

    public boolean isSuccessful() {
        return errorCode == 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + errorCode;
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QueryResult other = (QueryResult) obj;
        if (errorCode != other.errorCode)
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "query='" + query + '\'' +
                ", errorCode=" + errorCode +
                ", translation=" + Arrays.toString(translation) +
                ", message='" + message + '\'' +
                ", basicExplain=" + basicExplain +
                ", webExplains=" + Arrays.toString(webExplains) +
                '}';
    }

    public static String getErrorMessage(int errorCode) {
        return ERROR_MAP.get(errorCode);
    }

}
