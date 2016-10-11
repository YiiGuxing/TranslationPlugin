package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Settings
 */
@State(name = "TranslationSettings", storages = @Storage(id = "other", file = "$APP_CONFIG$/translation.xml"))
public class Settings implements PersistentStateComponent<Settings> {

    private boolean useDefaultKey = true;
    private String apiKeyName;
    private String apiKeyValue;

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

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public String getApiKeyValue() {
        return this.apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

}
