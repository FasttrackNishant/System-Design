package factory.actualfactory;

import factory.actualfactory.creator.NotificationCreator;

public class NotificationService {

    public void sendNotification(NotificationCreator creator , String message)
    {
        Notification notification = creator.createNotification();
        notification.send(message);
    }
}