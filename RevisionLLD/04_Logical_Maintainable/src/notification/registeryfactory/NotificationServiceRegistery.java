package notification.registeryfactory;

import java.util.*;
import java.util.function.Supplier;

/* =========================
   ENUMS
   ========================= */

enum NotificationType {
    SMS,
    EMAIL,
    PUSH
}

/* =========================
   DOMAIN CLASSES
   ========================= */

class Recipient {

    private final String userId;
    private final String phoneNumber;
    private final String email;
    private final String pushToken;

    public Recipient(String userId, String phoneNumber, String email, String pushToken) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.pushToken = pushToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPushToken() {
        return pushToken;
    }
}

/* =========================
   NOTIFICATION + BUILDER
   ========================= */

class Notification {

    private final String id;
    private final String message;
    private final String subject;
    private final NotificationType type;
    private final Recipient recipient;

    private Notification(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.message = builder.message;
        this.subject = builder.subject;
        this.type = builder.type;
        this.recipient = builder.recipient;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getSubject() {
        return subject;
    }

    public NotificationType getType() {
        return type;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public static class Builder {

        private final NotificationType type;
        private final Recipient recipient;
        private String message;
        private String subject;

        public Builder(NotificationType type, Recipient recipient) {
            this.type = type;
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

/* =========================
   STRATEGY: GATEWAY
   ========================= */

interface NotificationGateway {
    void send(Notification notification);
}

/* =========================
   CONCRETE GATEWAYS
   ========================= */

class SMSGateway implements NotificationGateway {

    @Override
    public void send(Notification notification) {
        System.out.println(
                "SMS sent to " + notification.getRecipient().getPhoneNumber()
                        + " | Message: " + notification.getMessage()
        );
    }
}

class EmailGateway implements NotificationGateway {

    @Override
    public void send(Notification notification) {
        System.out.println(
                "Email sent to " + notification.getRecipient().getEmail()
                        + " | Subject: " + notification.getSubject()
                        + " | Message: " + notification.getMessage()
        );
    }
}

/* =========================
   DECORATOR: RETRY
   ========================= */

class RetryDecorator implements NotificationGateway {

    private final int maxRetries;
    private final NotificationGateway gateway;

    public RetryDecorator(int maxRetries, NotificationGateway gateway) {
        this.maxRetries = maxRetries;
        this.gateway = gateway;
    }

    @Override
    public void send(Notification notification) {
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                gateway.send(notification);
                return;
            } catch (Exception ex) {
                attempts++;
                if (attempts == maxRetries) {
                    throw ex;
                }
            }
        }
    }
}

/* =========================
   FACTORY (REGISTRY-BASED)
   ========================= */

class NotificationFactory {

    private static final Map<NotificationType, Supplier<NotificationGateway>> registry =
            new EnumMap<>(NotificationType.class);

    static {
        registry.put(NotificationType.SMS, SMSGateway::new);
        registry.put(NotificationType.EMAIL, EmailGateway::new);
    }

    public static NotificationGateway getGateway(NotificationType type) {
        Supplier<NotificationGateway> supplier = registry.get(type);

        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported notification type: " + type);
        }
        return supplier.get();
    }
}

/* =========================
   SERVICE (COORDINATOR)
   ========================= */

class NotificationService {

    public void send(Notification notification) {

        validate(notification);

        NotificationGateway gateway =
                NotificationFactory.getGateway(notification.getType());

        NotificationGateway retryGateway =
                new RetryDecorator(3, gateway);

        retryGateway.send(notification);
    }

    private void validate(Notification notification) {

        Recipient recipient = notification.getRecipient();

        switch (notification.getType()) {
            case SMS:
                if (recipient.getPhoneNumber() == null) {
                    throw new IllegalArgumentException("Phone number required for SMS");
                }
                break;

            case EMAIL:
                if (recipient.getEmail() == null) {
                    throw new IllegalArgumentException("Email required for EMAIL");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported notification type");
        }
    }
}

/* =========================
   MAIN (DEMO)
   ========================= */

class NotificationSystemDemo {

    public static void main(String[] args) {

        Recipient recipient = new Recipient(
                "101",
                "9140000000",
                "dev@nishant.com",
                null
        );

        Notification notification =
                new Notification.Builder(NotificationType.SMS, recipient)
                        .subject("Interview Prep")
                        .message("Notification system design completed!")
                        .build();

        NotificationService service = new NotificationService();
        service.send(notification);
    }
}
