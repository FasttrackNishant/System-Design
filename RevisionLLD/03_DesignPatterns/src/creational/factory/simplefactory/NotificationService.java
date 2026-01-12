package creational.factory.simplefactory;

public class NotificationService {

    public void sendNotification(String type , String message){
        Notification notification = SimpleNotificationFactory.createNotificationFactory(type);
        notification.send(message);
    }
}