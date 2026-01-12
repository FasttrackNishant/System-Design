package creational.factory.actualfactory;

public class PushNotification implements Notification {

    @Override
    public void send(String message){
        System.out.println("Push Notification Send" + message);
    }
}