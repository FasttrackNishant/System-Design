package emailnotification.model;

public class SMSNotification implements Notification {

    @Override
    public void send(String message)
    {
        System.out.println("SMS Notification send"+message);
    }
}

