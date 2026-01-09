package followdip;

class EmailService {
    private final EmailClient emailClient; // Depends on the INTERFACE!

    // Dependency is "injected" via the constructor
    public EmailService(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void sendWelcomeEmail(String userEmail, String userName) {
        String subject = "Welcome, " + userName + "!";
        String body = "Thanks for signing up to our awesome platform. We're glad to have you!";
        this.emailClient.sendEmail(userEmail, subject, body); // Calls the interface method
    }

    public void sendPasswordResetEmail(String userEmail) {
        String subject = "Your Password Reset Request";
        String body = "Please click the link below to reset your password...";
        this.emailClient.sendEmail(userEmail, subject, body);
    }
}