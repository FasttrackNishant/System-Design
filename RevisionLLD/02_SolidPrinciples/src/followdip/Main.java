package followdip;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Using Gmail ---");
        EmailService gmailService = new EmailService(new GmailClientImpl());
        gmailService.sendWelcomeEmail("test@example.com", "Welcome to SOLID principles!");

        System.out.println("--- Using Outlook ---");
        EmailService outlookService = new EmailService(new OutlookClientImpl());
        outlookService.sendWelcomeEmail("test@example.com", "Welcome to SOLID principles!");
    }
}