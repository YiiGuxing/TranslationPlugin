package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.action.AutoSelectionMode;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings
 */
@State(name = "TranslationSettings", storages = @Storage(id = "other", file = "$APP_CONFIG$/translation.xml"))
public class Settings implements PersistentStateComponent<Settings> {

    private boolean useDefaultKey = true;
    private String apiKeyName;
    private String apiKeyValue;
    private boolean disableApiKeyNotification;
    @NotNull
    private AutoSelectionMode autoSelectionMode = AutoSelectionMode.INCLUSIVE;

    /**
     * Get the instance of this service.
     *
     * @return the unique {@link Settings} instance.
     */
    public static Settings getInstance() {
        return ServiceManager.getService(Settings.class);
    }

    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public String toString() {
        return "Settings{" +
                "useDefaultKey=" + useDefaultKey +
                ", apiKeyName='" + apiKeyName + '\'' +
                ", apiKeyValue='" + apiKeyValue + '\'' +
                ", disableApiKeyNotification=" + disableApiKeyNotification +
                ", autoSelectionMode=" + autoSelectionMode +
                '}';
    }

    /**
     * 返回自动取词模式
     */
    @NotNull
    public AutoSelectionMode getAutoSelectionMode() {
        return autoSelectionMode;
    }

    /**
     * 设置自动取词模式
     */
    public void setAutoSelectionMode(@NotNull AutoSelectionMode autoSelectionMode) {
        this.autoSelectionMode = autoSelectionMode;
    }

    /**
     * 返回是否是使用默认的API KEY.
     */
    public boolean isUseDefaultKey() {
        return useDefaultKey;
    }

    /**
     * 设置是否使用默认的API KEY.
     */
    public void setUseDefaultKey(boolean useDefaultKey) {
        this.useDefaultKey = useDefaultKey;
    }

    /**
     * 返回API KEY name.
     */
    public String getApiKeyName() {
        return this.apiKeyName;
    }

    /**
     * 设置API KEY name.
     */
    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    /**
     * 返回API KEY value.
     */
    public String getApiKeyValue() {
        return this.apiKeyValue;
    }

    /**
     * 设置API KEY value.
     */
    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public boolean isDisableApiKeyNotification() {
        return disableApiKeyNotification;
    }

    public void setDisableApiKeyNotification(boolean disableApiKeyNotification) {
        this.disableApiKeyNotification = disableApiKeyNotification;
    }
}
