package emailnotification.service;

import emailnotification.model.Notification;

public class NotificationService {

    public void sendEmailNotification(String message)
    {
        System.out.println("EMail Notification sent....! "+message);
    }

    // now two requirements sms and email

    public  void sendTwoNotifactions(String type,String message) {
        if (type.equals("email")) {
            System.out.println("Email Notification send" + message);
        } else if (type.equals("sms")) {
            System.out.println("Sms notification send" + message);
        }
    }

}
