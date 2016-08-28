package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.model.BasicExplain;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import cn.yiiguxing.plugin.translate.model.WebExplain;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

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


    static {
        StyleConstants.setItalic(ATTR_QUERY, true);
        StyleConstants.setBold(ATTR_QUERY, true);
        StyleConstants.setFontSize(ATTR_QUERY, JBUI.scaleFontSize(19));
        StyleConstants.setForeground(ATTR_QUERY, new JBColor(0xFFEE6000, 0xFFCC7832));

        StyleConstants.setForeground(ATTR_EXPLAIN, new JBColor(0xFF3E7EFF, 0xFF8CBCE1));

        StyleConstants.setItalic(ATTR_PRE_EXPLAINS, true);
        StyleConstants.setForeground(ATTR_PRE_EXPLAINS, new JBColor(0xFF7F0055, 0xFFEAB1FF));
        StyleConstants.setFontSize(ATTR_PRE_EXPLAINS, JBUI.scaleFontSize(16));

        StyleConstants.setForeground(ATTR_EXPLAINS, new JBColor(0xFF170591, 0xFFFFC66D));
        StyleConstants.setFontSize(ATTR_PRE_EXPLAINS, JBUI.scaleFontSize(16));

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

    public static void insertQueryResultText(@NotNull Document document, @NotNull QueryResult result) {
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

    private static void insertHeader(Document document, QueryResult result) {
        String query = result.getQuery();

        try {
            if (!Utils.isEmptyOrBlankString(query)) {
                query = query.trim();
                document.insertString(document.getLength(),
                        Character.toUpperCase(query.charAt(0)) + query.substring(1) + "\n", ATTR_QUERY);
            }

            BasicExplain be = result.getBasicExplain();
            if (be != null) {
                StringBuilder explain = new StringBuilder();

                String pho = be.getPhonetic();
                if (!isEmptyOrBlankString(pho)) {
                    explain.append("[");
                    explain.append(pho);
                    explain.append("]  ");
                }

                pho = be.getPhoneticUK();
                if (!isEmptyOrBlankString(pho)) {
                    explain.append("英[");
                    explain.append(pho);
                    explain.append("]  ");
                }

                pho = be.getPhoneticUS();
                if (!isEmptyOrBlankString(pho)) {
                    explain.append("美[");
                    explain.append(pho);
                    explain.append("]");
                }

                if (explain.length() > 0) {
                    document.insertString(document.getLength(), explain.toString() + "\n", ATTR_EXPLAIN);
                }
            }

            document.insertString(document.getLength(), "\n", null);
        } catch (BadLocationException e) {
            LOG.error("insertHeader ", e);
        }
    }

    private static void insertExplain(Document doc, String[] explains) {
        if (explains == null || explains.length == 0)
            return;

        try {
            for (String exp : explains) {
                if (isEmptyOrBlankString(exp))
                    continue;

                int i = exp.indexOf('.');
                if (i > 0) {
                    doc.insertString(doc.getLength(), exp.substring(0, i + 1), ATTR_PRE_EXPLAINS);
                    exp = exp.substring(i + 1);
                }

                doc.insertString(doc.getLength(), exp + '\n', ATTR_EXPLAINS);
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

}
