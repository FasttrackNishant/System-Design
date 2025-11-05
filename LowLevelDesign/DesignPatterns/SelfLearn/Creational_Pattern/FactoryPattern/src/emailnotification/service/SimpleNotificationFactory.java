package emailnotification.service;

import emailnotification.model.EmailNotification;
import emailnotification.model.Notification;
import emailnotification.model.SMSNotification;

public class SimpleNotificationFactory {

    public  static Notification createNotification(String type)
    {
        return switch (type)
        {
            case "email" -> new EmailNotification();
            case "sms" -> new SMSNotification();
            default -> throw new IllegalArgumentException("Unknown type");
        };
    }
}
