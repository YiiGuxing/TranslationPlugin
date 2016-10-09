package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.BasicExplain;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import cn.yiiguxing.plugin.translate.model.WebExplain;
import cn.yiiguxing.plugin.translate.ui.PhoneticButton;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.MouseEvent;

public final class Utils {

    @SuppressWarnings("SpellCheckingInspection")
    private static final Logger LOG = Logger.getInstance("#cn.yiiguxing.plugin.translate.Utils");

    private static final SimpleAttributeSet ATTR_QUERY = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_EXPLAIN = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_PRE_EXPLAINS = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_EXPLAINS = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_WEB_EXPLAIN_TITLE = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_WEB_EXPLAIN_KEY = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTR_WEB_EXPLAIN_VALUES = new SimpleAttributeSet();

    private static final int QUERY_FONT_SIZE = 19;
    private static final int PRE_EXPLAINS_FONT_SIZE = 16;
    private static final int EXPLAINS_FONT_SIZE = 16;

    static {
        StyleConstants.setItalic(ATTR_QUERY, true);
        StyleConstants.setBold(ATTR_QUERY, true);
        StyleConstants.setForeground(ATTR_QUERY, new JBColor(0xFFEE6000, 0xFFCC7832));

        StyleConstants.setForeground(ATTR_EXPLAIN, new JBColor(0xFF3E7EFF, 0xFF8CBCE1));

        StyleConstants.setItalic(ATTR_PRE_EXPLAINS, true);
        StyleConstants.setForeground(ATTR_PRE_EXPLAINS, new JBColor(0xFF7F0055, 0xFFEAB1FF));
        StyleConstants.setFontSize(ATTR_PRE_EXPLAINS, JBUI.scaleFontSize(16));

        StyleConstants.setForeground(ATTR_EXPLAINS, new JBColor(0xFF170591, 0xFFFFC66D));
        StyleConstants.setFontSize(ATTR_EXPLAINS, JBUI.scaleFontSize(16));

        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_TITLE, new JBColor(0xFF707070, 0xFF808080));
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_KEY, new JBColor(0xFF4C4C4C, 0xFF77B767));
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_VALUES, new JBColor(0xFF707070, 0xFF6A8759));
    }

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
                error = "Nothing to show";
                break;
        }

        return error;
    }

    public static void insertQueryResultText(@NotNull JTextPane textPane, @NotNull QueryResult result) {
        Document document = textPane.getDocument();
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return;
        }

        Utils.insertHeader(document, result);

        BasicExplain basicExplain = result.getBasicExplain();
        if (basicExplain != null) {
            Utils.insertExplain(document, basicExplain.getExplains());
        } else {
            Utils.insertExplain(document, result.getTranslation());
        }

        WebExplain[] webExplains = result.getWebExplains();
        Utils.insertWebExplain(document, webExplains);

        if (document.getLength() < 1)
            return;

        try {
            int offset = document.getLength() - 1;
            String text = document.getText(offset, 1);
            if (text.charAt(0) == '\n') {
                document.remove(offset, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 不能静态设置，否则scale改变时不能即时更新
    @NotNull
    private static MutableAttributeSet updateFontSize(@NotNull MutableAttributeSet attr, int size) {
        StyleConstants.setFontSize(attr, JBUI.scaleFontSize(size));
        return attr;
    }

    private static void insertHeader(@NotNull Document document, QueryResult result) {
        String query = result.getQuery();

        try {
            if (!Utils.isEmptyOrBlankString(query)) {
                query = query.trim();
                document.insertString(document.getLength(),
                        Character.toUpperCase(query.charAt(0)) + query.substring(1) + "\n",
                        updateFontSize(ATTR_QUERY, QUERY_FONT_SIZE));
            }

            BasicExplain be = result.getBasicExplain();
            if (be != null) {
                boolean hasPhonetic = false;

                String phoUK = be.getPhoneticUK();
                if (!isEmptyOrBlankString(phoUK)) {
                    insertPhonetic(document, result.getQuery(), phoUK, Speech.Phonetic.UK);
                    hasPhonetic = true;
                }

                String phoUS = be.getPhoneticUS();
                if (!isEmptyOrBlankString(phoUS)) {
                    insertPhonetic(document, result.getQuery(), phoUS, Speech.Phonetic.US);
                    hasPhonetic = true;
                }

                String pho = be.getPhonetic();
                if (!isEmptyOrBlankString(pho) && !hasPhonetic) {
                    document.insertString(document.getLength(), "[" + pho + "]", ATTR_EXPLAIN);
                    hasPhonetic = true;
                }

                if (hasPhonetic) {
                    document.insertString(document.getLength(), "\n", null);
                }
            }

            document.insertString(document.getLength(), "\n", null);
        } catch (BadLocationException e) {
            LOG.error("insertHeader ", e);
        }
    }

    private static void insertPhonetic(@NotNull Document document,
                                       @NotNull final String query,
                                       @NotNull String phoneticText,
                                       @NotNull final Speech.Phonetic phonetic) throws BadLocationException {
        String insert;
        if (phonetic == Speech.Phonetic.UK) {
            insert = "英[";
        } else {
            insert = "美[";
        }
        insert += phoneticText + "]";
        document.insertString(document.getLength(), insert, ATTR_EXPLAIN);

        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setComponent(attr, new PhoneticButton(new Consumer<MouseEvent>() {
            @Override
            public void consume(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 1) {
                    Speech.toSpeech(query, phonetic);
                }
            }
        }));
        document.insertString(document.getLength(), " ", attr);
    }

    private static void insertExplain(Document doc, String[] explains) {
        if (explains == null || explains.length == 0)
            return;

        final MutableAttributeSet attrPre = updateFontSize(ATTR_PRE_EXPLAINS, PRE_EXPLAINS_FONT_SIZE);
        final MutableAttributeSet attr = updateFontSize(ATTR_EXPLAINS, EXPLAINS_FONT_SIZE);
        try {
            for (String exp : explains) {
                if (isEmptyOrBlankString(exp))
                    continue;

                int i = exp.indexOf('.');
                if (i > 0) {
                    doc.insertString(doc.getLength(), exp.substring(0, i + 1), attrPre);
                    exp = exp.substring(i + 1);
                }

                doc.insertString(doc.getLength(), exp + '\n', attr);
            }

            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            LOG.error("insertExplain ", e);
        }
    }

    private static void insertWebExplain(Document doc, WebExplain[] webExplains) {
        if (webExplains == null || webExplains.length == 0)
            return;

        try {
            doc.insertString(doc.getLength(), "网络释义:\n", ATTR_WEB_EXPLAIN_TITLE);

            for (WebExplain webExplain : webExplains) {
                doc.insertString(doc.getLength(), webExplain.getKey(), ATTR_WEB_EXPLAIN_KEY);
                doc.insertString(doc.getLength(), " -", null);


                String[] values = webExplain.getValues();
                for (int i = 0; i < values.length; i++) {
                    doc.insertString(doc.getLength(), " " + values[i] + (i < values.length - 1 ? ";" : ""),
                            ATTR_WEB_EXPLAIN_VALUES);
                }
                doc.insertString(doc.getLength(), "\n", null);
            }

        } catch (BadLocationException e) {
            LOG.error("insertWebExplain ", e);
        }
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
