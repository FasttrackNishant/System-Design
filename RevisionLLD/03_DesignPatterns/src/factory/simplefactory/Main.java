package factory.simplefactory;

import factory.violation.NotificationService;

public class Main {

    public static void main(String[] args){

        NotificationService service  = new NotificationService();
        service.sendNotification("Email", "Email From Nishant");

    }
}
