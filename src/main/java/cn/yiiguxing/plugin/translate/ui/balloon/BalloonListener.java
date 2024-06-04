package cn.yiiguxing.plugin.translate.ui.balloon;

import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.util.messages.Topic;

@FunctionalInterface
public interface BalloonListener {

    void balloonShown(Balloon balloon);

    /**
     * Notification about showing balloon
     */
    @Topic.AppLevel
    Topic<BalloonListener> TOPIC = new Topic<>(BalloonListener.class, Topic.BroadcastDirection.TO_DIRECT_CHILDREN);
}
