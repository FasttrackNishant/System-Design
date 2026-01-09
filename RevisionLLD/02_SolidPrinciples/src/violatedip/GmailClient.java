package violatedip;

class GmailClient {
    public void sendGmail(String toAddress, String subjectLine, String emailBody) {
        System.out.println("Connecting to Gmail SMTP server...");
        System.out.println("Sending email via Gmail to: " + toAddress);
        System.out.println("Subject: " + subjectLine);
        System.out.println("Body: " + emailBody);
        // ... actual Gmail API interaction logic ...
        System.out.println("Gmail email sent successfully!");
    }
}