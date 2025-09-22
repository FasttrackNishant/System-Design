package emailnotification.creator;
import emailnotification.model.EmailNotification;
import emailnotification.model.Notification;

public class EmailNotificationCreator extends  NotificationCreator{

    @Override
    public Notification createNotification() {
        return  new EmailNotification();
    }
}
