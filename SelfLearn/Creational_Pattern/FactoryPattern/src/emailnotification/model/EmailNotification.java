package emailnotification.model;

public class EmailNotification implements Notification {
    @Override
    public void send(String message)
    {
        System.out.println("Email Notification sent !!!!!!!!!!!!"+message);
    }
}
