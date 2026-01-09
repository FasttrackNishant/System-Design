package factory.actualfactory.creator;

import factory.actualfactory.Notification;
import factory.actualfactory.PushNotification;

public class PushNotificationCreator extends NotificationCreator{

    @Override
    public Notification createNotification(){
        return new PushNotification();
    }
}
