package followdip;

interface EmailClient {
    void sendEmail(String to, String subject, String body);
}