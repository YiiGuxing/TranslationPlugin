package cn.yiiguxing.plugin.translate;

import com.intellij.notification.*;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class TranslationComponent extends AbstractProjectComponent {

    private static final String DISPLAY_ID_APP_KEY = "NOTIFICATION_APP_KEY";

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
        if (mSettings.isDisableAppKeyNotification() ||
                !Utils.isEmptyOrBlankString(mSettings.getAppId()) || mSettings.isPrivateKeyConfigured())
            return;

        NotificationGroup group = new NotificationGroup(DISPLAY_ID_APP_KEY, NotificationDisplayType.STICKY_BALLOON,
                true);
        String title = "设置App Key";
        String content = String.format("当前App Key为空或者无效，请设置App Key." +
                        "<br/><br/><a href=\"%s\">设置</a> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
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
                            mSettings.setDisableAppKeyNotification(true);
                        }
                    }
                });
        Notifications.Bus.notify(notification, myProject);
    }

}
