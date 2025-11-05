package emailnotification.creator;

import emailnotification.model.Notification;
import emailnotification.model.SMSNotification;

public class SMSNotificationCreator extends NotificationCreator{

    @Override
    public Notification createNotification() {
        return new SMSNotification();
    }
}
