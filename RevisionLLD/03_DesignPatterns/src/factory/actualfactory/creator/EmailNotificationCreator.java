package factory.actualfactory.creator;

import factory.actualfactory.EmailNotification;
import factory.actualfactory.Notification;
import factory.actualfactory.PushNotification;

public class EmailNotificationCreator extends NotificationCreator{

    @Override
    public Notification createNotification(){
        return new EmailNotification();
    }
}
