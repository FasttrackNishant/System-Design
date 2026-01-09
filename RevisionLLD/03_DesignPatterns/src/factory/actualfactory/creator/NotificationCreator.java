package factory.actualfactory.creator;

import factory.actualfactory.Notification;

public abstract class NotificationCreator {

    public abstract Notification createNotification();

    // optional
    public void sendNotification(String message){
        Notification notification = createNotification();
        notification.send(message);
    }

}
