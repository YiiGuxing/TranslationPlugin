package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.QueryResult;

public final class Utils {

    private Utils() {
    }

    public static String getErrorMessage(QueryResult result) {
        if (result == null)
            return "Nothing to show";

        if (result.getErrorCode() == QueryResult.ERROR_CODE_NONE)
            return null;

        String error;
        switch (result.getErrorCode()) {
            case QueryResult.ERROR_CODE_RESTRICTED:
                error = "请求过于频繁，请尝试更换API KEY";
                break;
            case QueryResult.ERROR_CODE_INVALID_KEY:
                error = "无效的API KEY";
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



    public static boolean isEmptyOrBlankString(String str) {
        return null == str || str.trim().length() == 0;
    }

    /**
     * 单词拆分
     */
    public static String splitWord(String input) {
        if (isEmptyOrBlankString(input))
            return input;

        return input.replaceAll("[_\\s]+", " ")
                .replaceAll("([A-Z][a-z]+)|([0-9\\W]+)", " $0 ")
                .replaceAll("[A-Z]{2,}", " $0")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * Checks that the specified object reference is not {@code null}. This
     * method is designed primarily for doing parameter validation in methods
     * and constructors, as demonstrated below:
     * <blockquote><pre>
     * public Foo(Bar bar) {
     *     this.bar = Objects.requireNonNull(bar);
     * }
     * </pre></blockquote>
     *
     * @param obj the object reference to check for nullity
     * @param <T> the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
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
     *     this.bar = Objects.requireNonNull(bar, "bar must not be null");
     *     this.baz = Objects.requireNonNull(baz, "baz must not be null");
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
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

}
