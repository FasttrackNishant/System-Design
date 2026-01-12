package creational.factory.actualfactory.creator;

import creational.factory.actualfactory.EmailNotification;
import creational.factory.actualfactory.Notification;

public class EmailNotificationCreator extends NotificationCreator{

    @Override
    public Notification createNotification(){
        return new EmailNotification();
    }
}
