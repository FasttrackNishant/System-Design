package easy.snakeandladder.java;

class RetryableGatewayDecorator implements NotificationGateway {
    private final NotificationGateway wrappedGateway;
    private final int maxRetries;
    private final long retryDelayMillis;

    public RetryableGatewayDecorator(NotificationGateway wrappedGateway, int maxRetries, long retryDelayMillis) {
        this.wrappedGateway = wrappedGateway;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public void send(Notification notification) throws Exception {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                wrappedGateway.send(notification);
                return; // Success
            } catch (Exception e) {
                attempt++;
                System.out.println("Error: Attempt " + attempt + " failed for notification " + notification.getId() + ". Retrying...");
                if (attempt >= maxRetries) {
                    System.out.println(e.getMessage());
                    throw new Exception("Failed to send notification after " + maxRetries + " attempts.", e);
                }
                Thread.sleep(retryDelayMillis);
            }
        }
    }
}















class Notification {
    private final String id;
    private final Recipient recipient;
    private final NotificationType type;
    private final String message;
    private final String subject; // Optional, for email

    private Notification(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.recipient = builder.recipient;
        this.type = builder.type;
        this.message = builder.message;
        this.subject = builder.subject;
    }

    // Getters
    public String getId() { return id; }
    public Recipient getRecipient() { return recipient; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public String getSubject() { return subject; }

    // Builder Class
    public static class Builder {
        private final Recipient recipient;
        private final NotificationType type;
        private String message;
        private String subject;

        public Builder(Recipient recipient, NotificationType type) {
            this.recipient = recipient;
            this.type = type;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}





class Recipient {
    private final String userId;
    private final Optional<String> email;
    private final Optional<String> phoneNumber;
    private final Optional<String> pushToken;

    public Recipient(String userId, String email, String phoneNumber, String pushToken) {
        this.userId = userId;
        this.email = Optional.ofNullable(email);
        this.phoneNumber = Optional.ofNullable(phoneNumber);
        this.pushToken = Optional.ofNullable(pushToken);
    }

    public String getUserId() {
        return userId;
    }

    public Optional<String> getEmail() {
        return email;
    }

    public Optional<String> getPhoneNumber() {
        return phoneNumber;
    }

    public Optional<String> getPushToken() {
        return pushToken;
    }
}







enum NotificationType {
    EMAIL,
    SMS,
    PUSH
}







class NotificationFactory {
    private static final Map<NotificationType, NotificationGateway> gatewayMap = new HashMap<>();

    public static NotificationGateway createGateway(NotificationType type) {
        if (gatewayMap.containsKey(type)) {
            return gatewayMap.get(type);
        }

        NotificationGateway gateway = null;

        switch (type) {
            case EMAIL:
                gateway = new EmailGateway();
                break;
            case SMS:
                gateway = new SmsGateway();
                break;
            case PUSH:
                gateway = new PushGateway();
                break;
        }

        gatewayMap.put(type, gateway);
        return gateway;
    }
}








class EmailGateway implements NotificationGateway {
    @Override
    public void send(Notification notification) {
        String email = notification.getRecipient().getEmail()
                .orElseThrow(() -> new IllegalArgumentException("Email address is required for EMAIL notification."));
        System.out.println("--- Sending EMAIL ---");
        System.out.println("To: " + email);
        System.out.println("Subject: " + notification.getSubject());
        System.out.println("Body: " + notification.getMessage());
        System.out.println("---------------------\n");
    }
}






interface NotificationGateway {
    void send(Notification notification) throws  Exception;
}









class PushGateway implements NotificationGateway {
    @Override
    public void send(Notification notification) {
        String token = notification.getRecipient().getPushToken()
                .orElseThrow(() -> new IllegalArgumentException("Push token is required for PUSH notification."));
        System.out.println("--- Sending PUSH Notification ---");
        System.out.println("To Device Token: " + token);
        System.out.println("Title: " + notification.getSubject()); // Re-using subject for title
        System.out.println("Body: " + notification.getMessage());
        System.out.println("---------------------------------\n");
    }
}








class SmsGateway implements NotificationGateway {
    @Override
    public void send(Notification notification) {
        String phone = notification.getRecipient().getPhoneNumber()
                .orElseThrow(() -> new IllegalArgumentException("Phone number is required for SMS notification."));
        System.out.println("--- Sending SMS ---");
        System.out.println("To: " + phone);
        System.out.println("Message: " + notification.getMessage());
        System.out.println("-------------------\n");
    }
}







class NotificationService {
    private final ExecutorService executor;

    public NotificationService(int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public void sendNotification(Notification notification) {
        executor.submit(() -> {
            NotificationGateway gateway = new RetryableGatewayDecorator(
                    NotificationFactory.createGateway(notification.getType()),
                    3,
                    1000);
            try {
                gateway.send(notification);
            } catch (Exception e) {
                System.out.println("Exception while sending notification: " + e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}










import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

public class NotificationSystemDemo {
    public static void main(String[] args) throws InterruptedException {
        // 1. Setup the notification service
        NotificationService notificationService = new NotificationService(10);

        // 2. Define recipients
        Recipient recipient1 = new Recipient("user123", "john.doe@example.com", null, "pushToken123");
        Recipient recipient2 = new Recipient("user456", null, "+15551234567", null);

        // 3. Send various notifications using the Facade (NotificationService)

        // Scenario 1: Send a welcome email
        Notification welcomeEmail = new Notification.Builder(recipient1, NotificationType.EMAIL)
                .subject("Welcome!")
                .message("Welcome to notification system")
                .build();
        notificationService.sendNotification(welcomeEmail);

        // Scenario 2: Send a direct push notification
        Notification pushNotification = new Notification.Builder(recipient1, NotificationType.PUSH)
                .subject("New Message")
                .message("You have a new message from Jane.")
                .build();
        notificationService.sendNotification(pushNotification);

        // Scenario 3: Send order confirmation SMS
        Notification orderSms = new Notification.Builder(recipient2, NotificationType.SMS)
                .message("Your order for Digital Clock is confirmed")
                .build();
        notificationService.sendNotification(orderSms);

        // Wait for a moment to allow the queue processor to work
        Thread.sleep(1000);

        // 4. Shutdown the system
        System.out.println("\nShutting down the notification system...");
        notificationService.shutdown();
        System.out.println("System shut down successfully.");
    }
}








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































