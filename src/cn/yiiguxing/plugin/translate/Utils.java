package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 辅助工具类
 */
@SuppressWarnings("WeakerAccess")
public final class Utils {

    private static final Pattern PATTERN_EXPLAIN =
            Pattern.compile("(^(a|adj|prep|pron|n|v|conj|s|sc|o|oc|vi|vt|aux|ad|adv|art|num|int|u|c|pl|abbr)\\.)(.+)");
    private static final int GROUP_CLASS = 1;
    private static final int GROUP_EXPLAIN = 3;

    private Utils() {
    }

    /**
     * 从翻译结果中获取错误信息
     *
     * @param result 翻译结果
     * @return 错误信息
     */
    public static String getErrorMessage(QueryResult result) {
        if (result == null)
            return "Nothing to show";

        if (result.getErrorCode() == QueryResult.ERROR_CODE_NONE)
            return null;

        String error;
        switch (result.getErrorCode()) {
            case QueryResult.ERROR_CODE_RESTRICTED:
                error = "请求过于频繁，请尝试<a href=\"" + Constants.HTML_DESCRIPTION_SETTINGS + "\">更换API KEY</a>";
                break;
            case QueryResult.ERROR_CODE_INVALID_KEY:
                error = "无效的API KEY,请<a href=\"" + Constants.HTML_DESCRIPTION_SETTINGS + "\">更换API KEY</a>";
                break;
            case QueryResult.ERROR_CODE_QUERY_TOO_LONG:
                error = "Query too long";
                break;
            case QueryResult.ERROR_CODE_UNSUPPORTED_LANG:
                error = "Unsupported lang";
                break;
            case QueryResult.ERROR_CODE_NO_RESULT:
            default:
                error = isEmptyOrBlankString(result.getMessage()) ? "Nothing to show" : result.getMessage();
                break;
        }

        return error;
    }

    /**
     * 测试目标字符串是否是空字符串或者空白字符串
     */
    public static boolean isEmptyOrBlankString(String str) {
        return null == str || str.trim().isEmpty();
    }

    /**
     * 单词拆分
     */
    public static String splitWord(String input) {
        if (isEmptyOrBlankString(input))
            return input;

        return input.replaceAll("[_*\\s]+", " ")
                .replaceAll("([A-Z][a-z]+)|([0-9\\W]+)", " $0 ")
                .replaceAll("[A-Z]{2,}", " $0")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * 拆分单词解释。例如：将 "vt. 分离; 使分离" 拆分为 ['vt.', '分离; 使分离']
     *
     * @return 一个长度为2的数组，数组第一个元素为单词词性，如果解释中不存在词性则为<code>null</code>。数组第二个元素为单词解释。
     */
    @NotNull
    public static String[] splitExplain(@NotNull String input) {
        String[] result = new String[2];
        Matcher explainMatcher = PATTERN_EXPLAIN.matcher(input);
        if (explainMatcher.find()) {
            result[0] = explainMatcher.group(GROUP_CLASS);
            result[1] = explainMatcher.group(GROUP_EXPLAIN).trim();
        } else {
            result[1] = input;
        }

        return result;
    }

    /**
     * 展开像 'Hello; Hi' 这样的解释
     */
    @Nullable
    public static String[] expandExplain(@Nullable String[] explains) {
        if (explains == null || explains.length == 0)
            return explains;

        final LinkedHashSet<String> result = new LinkedHashSet<String>(explains.length);
        final Pattern pattern = Pattern.compile("[;；]");
        for (String explain : explains) {
            Collections.addAll(result, pattern.split(explain));
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Checks that the specified object reference is not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar) {
     *     this.bar = Utils.requireNonNull(bar);
     * }
     * </pre></blockquote>
     *
     * @param obj the object reference to check for nullity
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @NotNull
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    /**
     * Checks that the specified object reference is not {@code null} and
     * throws a customized {@link NullPointerException} if it is. This method
     * is designed primarily for doing parameter validation in methods and
     * constructors with multiple parameters, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar, Baz baz) {
     *     this.bar = Utils.requireNonNull(bar, "bar must not be null");
     *     this.baz = Utils.requireNonNull(baz, "baz must not be null");
     * }
     * </pre></blockquote>
     *
     * @param obj     the object reference to check for nullity
     * @param message detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @param <T>     the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    @SuppressWarnings("SameParameterValue")
    @NotNull
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

    /**
     * Next four methods are created for new version API Url Generating.
     * Usage: input appKey, password and query target in order to generate a query url.
     */
    public static String generateRequestUrl(String appKey,String passwd, String query) {//TODO: passwd is a temp name of "密钥"
        String appKey = appKey;
        String query = query;
        String salt = String.valueOf(System.currentTimeMillis());
        String from = "auto";
        String to = "auto";
        String sign = md5(appKey + query + salt+ passwd);
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", query);
        params.put("from", from);
        params.put("to", to);
        params.put("sign", sign);
        params.put("salt", salt);
        params.put("appKey", appKey);
        return getUrlWithQueryString("https://openapi.youdao.com/api", params);
    }

    /**
     * 生成32位MD5摘要
     * @param string
     * @return
     */
    private static String md5(String string) {
        if(string == null){
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes();
        try{
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
        }catch(NoSuchAlgorithmException e){
            return null;
        }
    }

    /**
     * 根据api地址和参数生成请求URL
     * @param url
     * @param params
     * @return
     */
    private static String getUrlWithQueryString(String url, Map<String, String> params) {
        if (params == null) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url);
        if (url.contains("?")) {
            builder.append("&");
        } else {
            builder.append("?");
        }

        int i = 0;
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null) { // 过滤空的key
                continue;
            }

            if (i != 0) {
                builder.append('&');
            }

            builder.append(key);
            builder.append('=');
            builder.append(encode(value));

            i++;
        }

        return builder.toString();
    }
    /**
     * 进行URL编码
     * @param input
     * @return
     */
    private static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return input;
    }
}
