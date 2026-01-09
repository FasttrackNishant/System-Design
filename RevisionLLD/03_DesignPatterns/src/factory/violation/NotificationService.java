package factory.violation;

public class NotificationService{

    public void sendNotification(String type , String message){

        if(type.equals("Email")){
            EmailNotification notification = new EmailNotification();
            notification.send(message);
        }
        else if(type.equals("Whatsapp")){
            WhatsappNotification notification = new WhatsappNotification();
            notification.send(message);
        }

    }
}