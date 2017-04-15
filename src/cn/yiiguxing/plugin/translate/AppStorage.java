package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.CollectionBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 插件应用数据存储
 */
@State(name = "AppStorage", storages = @Storage(id = "other", file = "$APP_CONFIG$/storage_data.xml"))
public class AppStorage implements PersistentStateComponent<AppStorage> {

    private static final int DEFAULT_HISTORY_SIZE = 50;
    private static final int MAX_HISTORY_SIZE = 200;

    @CollectionBean
    private List<String> histories = new ArrayList<String>(DEFAULT_HISTORY_SIZE);
    private int maxSize = DEFAULT_HISTORY_SIZE;

    /**
     * Get the instance of this service.
     *
     * @return the unique {@link AppStorage} instance.
     */
    public static AppStorage getInstance() {
        return ServiceManager.getService(AppStorage.class);
    }

    @Nullable
    @Override
    public AppStorage getState() {
        return this;
    }

    @Override
    public void loadState(AppStorage state) {
        if (state.histories == null) {
            state.histories = histories;
        }

        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String toString() {
        return "AppStorage{" +
                "histories=" + histories +
                ", maxSize=" + maxSize +
                '}';
    }

    /**
     * 设置最大历史启启记录长度
     *
     * @param size 最大历史启启记录长度
     */
    public void setMaxHistoriesSize(int size) {
        size = Math.max(0, Math.min(size, MAX_HISTORY_SIZE));
        if (size != maxSize) {
            maxSize = size;
            int listSize = histories.size();
            boolean trimmed = listSize > maxSize;

            if (maxSize == 0) {
                histories.clear();
            } else {
                while (listSize > maxSize) {
                    histories.remove(--listSize);
                }
            }

            if (trimmed) {
                ApplicationManager
                        .getApplication()
                        .getMessageBus()
                        .syncPublisher(HistoriesChangedListener.TOPIC)
                        .onHistoriesChanged();
            }
        }
    }

    /**
     * @return 最大历史启启记录长度
     */
    public int getMaxHistorySize() {
        return maxSize;
    }

    /**
     * @return 历史上记录列表
     */
    @NotNull
    public List<String> getHistories() {
        return Collections.unmodifiableList(this.histories);
    }

    /**
     * 添加历史记录
     *
     * @param query 查询
     * @return <code>true</code> - 添加成功。<code>false</code> - 其他。
     */
    public boolean addHistory(@NotNull String query) {
        if (maxSize > 0) {
            int index = histories.indexOf(query);
            if (index != 0) {
                if (index > 0) {
                    histories.remove(index);
                }
                if (histories.size() >= maxSize) {
                    histories.remove(maxSize - 1);
                }

                histories.add(0, query);

                ApplicationManager
                        .getApplication()
                        .getMessageBus()
                        .syncPublisher(HistoriesChangedListener.TOPIC)
                        .onHistoryItemChanged(query);

                return true;
            }
        }

        return false;
    }

    /**
     * 清除历史启启记录
     */
    public void clearHistories() {
        if (!histories.isEmpty()) {
            histories.clear();

            ApplicationManager
                    .getApplication()
                    .getMessageBus()
                    .syncPublisher(HistoriesChangedListener.TOPIC)
                    .onHistoriesChanged();
        }
    }


    public interface HistoriesChangedListener {
        Topic<HistoriesChangedListener> TOPIC =
                Topic.create("TranslateHistoriesChanged", HistoriesChangedListener.class);

        void onHistoriesChanged();

        void onHistoryItemChanged(@NotNull String newHistory);
    }

}
