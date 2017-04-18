package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.FontInfo;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FontComboBox extends ComboBox {
    private static final String PHONETIC_SIMPLE = "ːiɜɑɔuɪeæʌɒʊəaɛpbtdkgfvszθðʃʒrzmnŋhljw";
    private static final FontInfoRenderer RENDERER = new FontInfoRenderer();

    private Model myModel;

    public FontComboBox() {
        this(false);
    }

    public FontComboBox(boolean withAllStyles) {
        this(withAllStyles, false);
    }

    public FontComboBox(boolean withAllStyles, boolean filterNonPhonetic) {
        super(new Model(withAllStyles, filterNonPhonetic));
        Dimension size = getPreferredSize();
        size.width = size.height * 8;
        setPreferredSize(size);
        setSwingPopup(true);
        setRenderer(RENDERER);
    }

    public boolean isMonospacedOnly() {
        return myModel.myMonospacedOnly;
    }

    public void setMonospacedOnly(boolean monospaced) {
        if (myModel.myMonospacedOnly != monospaced) {
            myModel.myMonospacedOnly = monospaced;
            myModel.updateSelectedItem();
        }
    }

    public String getFontName() {
        Object item = myModel.getSelectedItem();
        return item == null ? null : item.toString();
    }

    public void setFontName(String item) {
        myModel.setSelectedItem(item);
    }

    @Override
    public void setModel(ComboBoxModel model) {
        if (model instanceof Model) {
            myModel = (Model) model;
            super.setModel(model);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Model extends AbstractListModel implements ComboBoxModel {
        private volatile List<FontInfo> myAllFonts = Collections.emptyList();
        private volatile List<FontInfo> myMonoFonts = Collections.emptyList();
        private boolean myMonospacedOnly;
        private Object mySelectedItem;

        private Model(final boolean withAllStyles, final boolean filterNonPhonetic) {
            final Application application = ApplicationManager.getApplication();
            if (application == null || application.isUnitTestMode()) {
                setFonts(FontInfo.getAll(withAllStyles), filterNonPhonetic);
            } else {
                application.executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        final List<FontInfo> all = FontInfo.getAll(withAllStyles);
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setFonts(all, filterNonPhonetic);
                                updateSelectedItem();
                            }
                        }, application.getAnyModalityState());
                    }
                });
            }
        }

        private void setFonts(List<FontInfo> all, boolean filterNonPhonetic) {
            List<FontInfo> allFonts = new ArrayList<FontInfo>(all.size());
            List<FontInfo> monoFonts = new ArrayList<FontInfo>();
            for (FontInfo info : all) {
                if (!filterNonPhonetic || info.getFont().canDisplayUpTo(PHONETIC_SIMPLE) == -1) {
                    allFonts.add(info);
                    if (info.isMonospaced()) {
                        monoFonts.add(info);
                    }
                }
            }
            myAllFonts = allFonts;
            myMonoFonts = monoFonts;
        }

        private void updateSelectedItem() {
            Object item = getSelectedItem();
            setSelectedItem(null);
            setSelectedItem(item);
        }

        @Override
        public Object getSelectedItem() {
            return mySelectedItem;
        }

        @Override
        public void setSelectedItem(Object item) {
            if (item instanceof FontInfo) {
                FontInfo info = getInfo(item);
                if (info == null) {
                    List<FontInfo> list = myMonospacedOnly ? myMonoFonts : myAllFonts;
                    item = list.isEmpty() ? null : list.get(0);
                }
            }
            if (item instanceof String) {
                FontInfo info = getInfo(item);
                if (info != null) item = info;
            }
            if (!(mySelectedItem == null ? item == null : mySelectedItem.equals(item))) {
                mySelectedItem = item;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public int getSize() {
            List<FontInfo> list = myMonospacedOnly ? myMonoFonts : myAllFonts;
            return mySelectedItem instanceof String ? 1 + list.size() : list.size();
        }

        @Override
        public Object getElementAt(int index) {
            List<FontInfo> list = myMonospacedOnly ? myMonoFonts : myAllFonts;
            return 0 <= index && index < list.size() ? list.get(index) : mySelectedItem;
        }

        private FontInfo getInfo(Object item) {
            for (FontInfo info : myMonospacedOnly ? myMonoFonts : myAllFonts) {
                if (item instanceof String ? info.toString().equalsIgnoreCase((String) item) : info.equals(item)) {
                    return info;
                }
            }
            return null;
        }
    }

    private static final class FontInfoRenderer extends ColoredListCellRenderer<Object> {
        @Override
        protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean focused) {
            Font font = list.getFont();
            String text = value == null ? "" : value.toString();
            append(text);
            if (value instanceof FontInfo) {
                FontInfo info = (FontInfo) value;
                Integer size = getFontSize();
                Font f = info.getFont(size != null ? size : font.getSize());
                if (f.canDisplayUpTo(text) == -1) {
                    setFont(f);
                } else {
                    append("  Non-latin", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        }

        @Override
        protected void applyAdditionalHints(@NotNull Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, AntialiasingType.getKeyForCurrentScope(isEditorFont()));
        }

        Integer getFontSize() {
            return null;
        }

        boolean isEditorFont() {
            return false;
        }
    }

}
