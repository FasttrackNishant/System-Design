package creational.factory.actualfactory.creator;

import creational.factory.actualfactory.Notification;
import creational.factory.actualfactory.PushNotification;

public class PushNotificationCreator extends NotificationCreator{

    @Override
    public Notification createNotification(){
        return new PushNotification();
    }
}
