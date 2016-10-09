package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.Nullable;

/**
 * Settings
 */
@State(name = "TranslationSettings", storages = @Storage(id = "other", file = "$APP_CONFIG$/translation.xml"))
public class Settings implements PersistentStateComponent<Settings.State> {

    static class State {
        public boolean useDefaultKey = true;
        public String apiKeyName;
        public String apiKeyValue;
    }

    private State state;

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
    public State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
    }

    public boolean isUseDefaultKey() {
        return state.useDefaultKey;
    }

    public void setUseDefaultKey(boolean useDefaultKey) {
        state.useDefaultKey = useDefaultKey;
    }

    public String getApiKeyName() {
        return state.apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        state.apiKeyName = apiKeyName;
    }

    public String getApiKeyValue() {
        return state.apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        state.apiKeyValue = apiKeyValue;
    }

}
