package cn.yiiguxing.plugin.translate;

import com.intellij.notification.*;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class TranslationComponent extends AbstractProjectComponent {

    private static final String DISPLAY_ID_API_KEY = "NOTIFICATION_API_KEY";

    private Settings mSettings;

    public TranslationComponent(Project project) {
        super(project);
    }

    @Override
    public void initComponent() {
        super.initComponent();
        mSettings = Settings.getInstance();
    }

    @Override
    public void projectOpened() {
        if (mSettings.isDisableApiKeyNotification() || !mSettings.isUseDefaultKey())
            return;

        NotificationGroup group = new NotificationGroup(DISPLAY_ID_API_KEY, NotificationDisplayType.STICKY_BALLOON,
                true);
        String title = "避免使用公共API Key";
        String content = String.format("你正在使用公共的API Key，这可能会导致无法正常地进行翻译，建议改用个人API Key." +
                        "<br/><br/><a href=\"%s\">更改API Key</a> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                        "<a href=\"%s\">不再提示</a>",
                Constants.HTML_DESCRIPTION_SETTINGS, Constants.HTML_DESCRIPTION_DISABLE);
        Notification notification = group.createNotification(title, content, NotificationType.WARNING,
                new NotificationListener.Adapter() {
                    @Override
                    protected void hyperlinkActivated(@NotNull Notification notification,
                                                      @NotNull HyperlinkEvent hyperlinkEvent) {
                        notification.expire();

                        final String description = hyperlinkEvent.getDescription();
                        if (Constants.HTML_DESCRIPTION_SETTINGS.equals(description)) {
                            TranslationOptionsConfigurable.showSettingsDialog(myProject);
                        } else if (Constants.HTML_DESCRIPTION_DISABLE.equals(description)) {
                            mSettings.setDisableApiKeyNotification(true);
                        }
                    }
                });
        Notifications.Bus.notify(notification, myProject);
    }

}
