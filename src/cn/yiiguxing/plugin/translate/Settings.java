package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.action.AutoSelectionMode;
import cn.yiiguxing.plugin.translate.compat.PasswordSafeCompat;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Settings
 */
@SuppressWarnings("WeakerAccess")
@State(name = "TranslationSettings", storages = @Storage(id = "other", file = "$APP_CONFIG$/translation.xml"))
public class Settings implements PersistentStateComponent<Settings> {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String YOUDAO_APP_PRIVATE_KEY = "YOUDAO_APP_PRIVATE_KEY";

    private String appId;
    private Lang langFrom;
    private Lang langTo;
    private boolean privateKeyConfigured;
    private boolean overrideFont;
    private String primaryFontFamily;
    private String phoneticFontFamily;
    private boolean disableAppKeyNotification;
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
                "appId='" + appId + '\'' +
                ", langFrom=" + langFrom +
                ", langTo=" + langTo +
                ", privateKeyConfigured=" + privateKeyConfigured +
                ", overrideFont=" + overrideFont +
                ", primaryFontFamily='" + primaryFontFamily + '\'' +
                ", phoneticFontFamily='" + phoneticFontFamily + '\'' +
                ", disableAppKeyNotification=" + disableAppKeyNotification +
                ", autoSelectionMode=" + autoSelectionMode +
                '}';
    }

    /**
     * 返回输入语言
     */
    @Nullable
    public Lang getLangFrom() {
        return langFrom;
    }

    /**
     * 设置输入语言
     *
     * @param lang 语言
     */
    public void setLangFrom(@Nullable Lang lang) {
        this.langFrom = lang;
    }

    /**
     * 返回输出语言
     */
    @Nullable
    public Lang getLangTo() {
        return langTo;
    }

    /**
     * 设置输出语言
     *
     * @param lang 语言
     */
    public void setLangTo(@Nullable Lang lang) {
        this.langTo = lang;
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
     * 返回应用ID.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * 设置应用ID.
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public boolean isPrivateKeyConfigured() {
        return privateKeyConfigured;
    }

    public void setPrivateKeyConfigured(boolean privateKeyConfigured) {
        this.privateKeyConfigured = privateKeyConfigured;
    }

    /**
     * 返回应用密钥.
     */
    @SuppressWarnings("deprecation")
    @Transient
    @NotNull
    public String getAppPrivateKey() {
        return StringUtil.notNullize(PasswordSafeCompat.getPassword(Settings.class, YOUDAO_APP_PRIVATE_KEY));
    }

    /**
     * 设置应用密钥.
     */
    @Transient
    @SuppressWarnings("deprecation")
    public void setAppPrivateKey(@NotNull String appPrivateKey) {
        setPrivateKeyConfigured(!appPrivateKey.isEmpty());
        PasswordSafeCompat.setPassword(Settings.class, YOUDAO_APP_PRIVATE_KEY, appPrivateKey);
    }

    /**
     * 返回是否关闭设置APP KEY通知
     */
    public boolean isDisableAppKeyNotification() {
        return disableAppKeyNotification;
    }

    /**
     * 设置关闭设置APP KEY通知
     */
    @SuppressWarnings("SameParameterValue")
    public void setDisableAppKeyNotification(boolean disableAppKeyNotification) {
        this.disableAppKeyNotification = disableAppKeyNotification;
    }

    /**
     * 返回是否覆盖默认字体
     */
    public boolean isOverrideFont() {
        return overrideFont;
    }

    /**
     * 设置是否覆盖默认字体
     */
    public void setOverrideFont(boolean overrideFont) {
        if (this.overrideFont != overrideFont) {
            this.overrideFont = overrideFont;

            getSettingsChangePublisher().onOverrideFontChanged(this);
        }
    }

    public String getPrimaryFontFamily() {
        return primaryFontFamily;
    }

    public void setPrimaryFontFamily(String primaryFontFamily) {
        if (this.primaryFontFamily != null
                ? !this.primaryFontFamily.equals(primaryFontFamily) : primaryFontFamily != null) {
            this.primaryFontFamily = primaryFontFamily;

            getSettingsChangePublisher().onOverrideFontChanged(this);
        }
    }

    public String getPhoneticFontFamily() {
        return phoneticFontFamily;
    }

    public void setPhoneticFontFamily(String phoneticFontFamily) {
        if (this.phoneticFontFamily != null
                ? !this.phoneticFontFamily.equals(phoneticFontFamily) : phoneticFontFamily != null) {
            this.phoneticFontFamily = phoneticFontFamily;

            getSettingsChangePublisher().onOverrideFontChanged(this);
        }
    }

    @NotNull
    private static SettingsChangeListener getSettingsChangePublisher() {
        return ApplicationManager
                .getApplication()
                .getMessageBus()
                .syncPublisher(SettingsChangeListener.TOPIC);
    }

    public interface SettingsChangeListener {
        Topic<SettingsChangeListener> TOPIC =
                Topic.create("TranslationSettingsChanged", SettingsChangeListener.class);

        void onOverrideFontChanged(@NotNull Settings settings);
    }
}
