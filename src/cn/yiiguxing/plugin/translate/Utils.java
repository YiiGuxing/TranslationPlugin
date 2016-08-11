package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

final class Utils {

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
        StyleConstants.setFontSize(ATTR_QUERY, 18);
        StyleConstants.setForeground(ATTR_QUERY, new JBColor(0xFF333333, 0xFFCC7832));

        StyleConstants.setForeground(ATTR_EXPLAIN, new JBColor(0xFF333333, 0xFF8CBCE1));

        StyleConstants.setItalic(ATTR_PRE_EXPLAINS, true);
        StyleConstants.setForeground(ATTR_PRE_EXPLAINS, new JBColor(0xFF333333, 0xFFEAB1FF));

        StyleConstants.setForeground(ATTR_EXPLAINS, new JBColor(0xFF333333, 0xFFFFC66D));

        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_TITLE, new JBColor(0xFF333333, 0xFF808080));
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_KEY, new JBColor(0xFF333333, 0xFF77B767));
        StyleConstants.setForeground(ATTR_WEB_EXPLAIN_VALUES, new JBColor(0xFF333333, 0xFF6A8759));
    }

    private Utils() {
    }

    static String getErrorMessage(QueryResult result) {
        if (result == null)
            return "Nothing to show";

        if (result.getErrorCode() == QueryResult.ERROR_CODE_NONE)
            return null;

        String error;
        switch (result.getErrorCode()) {
            case QueryResult.ERROR_CODE_QUERY_TOO_LONG:
                error = "Query too long";
                break;
            case QueryResult.ERROR_CODE_INVALID_KEY:
                error = "Invalid key";
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

    static void insertHeader(Document document, QueryResult result) {
        String query = result.getQuery();

        try {
            document.insertString(document.getLength(), "  " + query + "  \n", ATTR_QUERY);

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

                document.insertString(document.getLength(), explain.toString() + "\n", ATTR_EXPLAIN);
            }

            document.insertString(document.getLength(), "\n", null);
        } catch (BadLocationException e) {
            LOG.error("insertHeader ", e);
        }
    }

    static void insertExplain(Document doc, String[] explains) {
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

    static void insertWebExplain(Document doc, WebExplain[] webExplains) {
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

    static boolean isEmptyOrBlankString(String str) {
        return null == str || str.trim().length() == 0;
    }

    /**
     * 单词拆分
     */
    static String splitWord(String input) {
        if (isEmptyOrBlankString(input))
            return input;

        return input.replace("_", " ")
                .replaceAll("([A-Z][a-z]+)|([0-9\\W]+)", " $0 ")
                .replaceAll("[A-Z]{2,}", " $0")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

}
