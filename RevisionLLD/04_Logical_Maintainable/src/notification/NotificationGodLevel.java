package notification;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

class NotificationGodLevel {
}

enum NotificationType {
    SMS,
    PUSH,
    EMAIL
}


class Notification {

    private String id;
    private String message;
    private String subject;
    private NotificationType type;
    private Recipient recipient;

    public String getMessage() {
        return message;
    }

    public String getId() {
        return id;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public NotificationType getType() {
        return type;
    }

    public Notification(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.message = builder.message;
        this.subject = builder.subject;
        this.recipient = builder.recipient;
        this.type = builder.notificationType;

    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", subject='" + subject + '\'' +
                ", type=" + type +
                ", recipient=" + recipient +
                '}';
    }

    public static class Builder {

        private String message;
        private String subject;
        private NotificationType notificationType;
        private Recipient recipient;

        public Builder(NotificationType notificationType, Recipient recipient) {
            this.notificationType = notificationType;
            this.recipient = recipient;
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

    private String userId;
    private Optional<String> phoneNumber;
    private Optional<String> email;
    private Optional<String> pushToken;

    public Recipient(String userId, String phoneNumber, String email, String pushToken) {
        this.userId = userId;
        this.phoneNumber = Optional.ofNullable(phoneNumber);
        this.email = Optional.ofNullable(email);
        this.pushToken = Optional.of(pushToken);
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

    public String getUserId() {
        return userId;
    }
}

interface NotificationGateway {
    void sendNotification(Notification notification);
}

class SMSGateway implements NotificationGateway {

    @Override
    public void sendNotification(Notification notification) {
        System.out.println("SMS Notification Send to " + notification.getRecipient().getPhoneNumber());
        System.out.println("Message for user " + notification.getMessage());
    }
}

class EmailGateway implements NotificationGateway {

    @Override
    public void sendNotification(Notification notification) {
        System.out.println("Email Send to " + notification.getRecipient().getEmail());
        System.out.println(notification.getSubject() + " " + notification.getMessage());
    }
}

class RetryDecorater implements NotificationGateway {

    private int retryCount;
    private NotificationGateway gateway;

    public RetryDecorater(int retryCount, NotificationGateway gateway) {
        this.retryCount = retryCount;
        this.gateway = gateway;
    }


    @Override
    public void sendNotification(Notification notification) {

        int attempt = 0;

        while (attempt < retryCount) {
            try {
                System.out.println("in decorater");
                throw new InterruptedException("ruk gaya");
                //gateway.sendNotification(notification);
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            } finally {
                attempt++;
            }
        }

    }
}


class NotificationFactory {

    private static Map<NotificationType, NotificationGateway> gatewayMap = new HashMap<>();

    public static NotificationGateway createNotificationGateway(NotificationType type) {

        if (gatewayMap.containsKey(type)) {
            return gatewayMap.get(type);
        }

        NotificationGateway gateway = null;
        System.out.println(type);
        switch (type) {
            case SMS:
                gateway = new SMSGateway();
                break;
            case EMAIL:
                gateway = new EmailGateway();
                break;
        }

        gatewayMap.put(type, gateway);
        return gateway;

    }
}


class NotificationService {

    void sendNotification(Notification notification) {
        System.out.println(notification);
        NotificationGateway gateway = NotificationFactory.createNotificationGateway(notification.getType());
        gateway = new RetryDecorater(5, gateway);
        gateway.sendNotification(notification);

    }
}

class Main {
    public static void main(String[] args) {

        Recipient recipient = new Recipient("101", "9140", "devnishant", "1234");
        NotificationService service = new NotificationService();
        Notification notification = new Notification.Builder(NotificationType.EMAIL, recipient)
                .message("Hi Nishant here")
                .subject("Testing notification")
                .build();

        service.sendNotification(notification);


    }
}