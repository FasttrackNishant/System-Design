import emailnotification.creator.EmailNotificationCreator;
import emailnotification.creator.NotificationCreator;
import emailnotification.creator.SMSNotificationCreator;
import emailnotification.model.Notification;
import emailnotification.service.NotificationService;
import emailnotification.service.SimpleNotificationFactory;

public class Main {
    public static void main(String[] args) {

        // 1 St Normal Requirement
        NotificationService emailNotification = new NotificationService();
        emailNotification.sendEmailNotification("Sent Mail to Admin");

        // Now for two
        emailNotification.sendTwoNotifactions("email","Hi via email");
        emailNotification.sendTwoNotifactions("sms","Hi via sms");

        //Using Simple Factory

        String type = "email";
        String message = "nishant email";
        Notification notificationSimpFact = SimpleNotificationFactory.createNotification(type);
        notificationSimpFact.send(message);

        // Using Creator Method
        NotificationCreator creator;

        creator = new EmailNotificationCreator();
        creator.send("Welcome to design pattern");

        creator = new SMSNotificationCreator();
        creator.send("This is new SMS Notification");

    }
}