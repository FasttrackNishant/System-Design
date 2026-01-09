package factory.actualfactory;

import factory.actualfactory.creator.EmailNotificationCreator;
import factory.actualfactory.creator.NotificationCreator;
import factory.actualfactory.creator.PushNotificationCreator;

public class Main {

    public static void main(String[] args){

        NotificationCreator emailCreator = new EmailNotificationCreator();
        NotificationCreator pushCreator = new PushNotificationCreator();

        // using creator
        pushCreator.sendNotification("VIP Entry from Nishant");

        NotificationService service  = new NotificationService();
        service.sendNotification(emailCreator, "Message From Nishant");

        service.sendNotification(pushCreator, "Message From Nishant");

    }
}
