package creational.factory.violation;

public class Main {

    public static void main(String[] args){
        NotificationService service = new NotificationService();
        service.sendNotification("Email","Hi This is Nishant from Email");
        service.sendNotification("Whatsapp","Hi This is Nishant from Whatsapp");

    }
}
