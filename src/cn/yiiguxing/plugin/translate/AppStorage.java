package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
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

    private static final int HISTORY_SIZE = 50;

    @CollectionBean
    private List<String> histories = new ArrayList<String>(HISTORY_SIZE);

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
                '}';
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
        int index = histories.indexOf(query);
        if (index != 0) {
            if (index > 0) {
                histories.remove(index);
            }
            if (histories.size() >= HISTORY_SIZE) {
                histories.remove(HISTORY_SIZE - 1);
            }

            histories.add(0, query);
            return true;
        }

        return false;
    }

}
