package creational.factory.simplefactory;

public class SimpleNotificationFactory{

    public static Notification createNotificationFactory(String type){

        // Here OCP is Violated
        return switch(type){
            case "EMAIL" -> new EmailNotification();
            case "PUSH" -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown Notification type");
        };
    }
}