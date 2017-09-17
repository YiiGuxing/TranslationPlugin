package cn.yiiguxing.plugin.translate.util;

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
     * 测试目标字符串是否是空字符串或者空白字符串
     */
    public static boolean isEmptyOrBlankString(String str) {
        return null == str || str.trim().isEmpty();
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

    @NotNull
    public static <T> T notNull(@Nullable T value, @NotNull T defaultValue) {
        return value == null ? defaultValue : value;
    }

}
