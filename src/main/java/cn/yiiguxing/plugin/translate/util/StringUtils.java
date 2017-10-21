package cn.yiiguxing.plugin.translate.util;

/**
 * 辅助工具类
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 测试目标字符串是否是空字符串或者空白字符串
     */
    public static boolean isEmptyOrBlankString(String str) {
        return null == str || str.trim().isEmpty();
    }

}
