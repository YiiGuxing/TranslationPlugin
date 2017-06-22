package cn.yiiguxing.plugin.translate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    @NotNull
    public static <T> T notNull(@Nullable T value, @NotNull T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * 生成32位MD5摘要
     *
     * @param input 输入
     * @return MD5摘要
     */
    @Nullable
    public static String md5(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(input.getBytes(Charset.forName("UTF-8")));
            byte[] md = mdInst.digest();
            char str[] = new char[md.length * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String urlEncode(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /* no-op */
        }

        return input;
    }

}
