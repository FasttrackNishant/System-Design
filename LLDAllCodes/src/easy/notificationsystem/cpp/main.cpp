
class RetryableGatewayDecorator : public NotificationGateway {
private:
    NotificationGateway* wrappedGateway;
    int maxRetries;

public:
    RetryableGatewayDecorator(NotificationGateway* wrappedGateway, int maxRetries)
        : wrappedGateway(wrappedGateway), maxRetries(maxRetries) {}

    void send(const Notification& notification) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                wrappedGateway->send(notification);
                return; // Success
            } catch (const exception& e) {
                attempt++;
                cout << "Error: Attempt " << attempt << " failed for notification " << notification.getId() << ". Retrying..." << endl;
                if (attempt >= maxRetries) {
                    cout << e.what() << endl;
                    throw runtime_error("Failed to send notification after " + to_string(maxRetries) + " attempts.");
                }
                // Simple delay simulation without actual sleep
                cout << "Retrying after delay..." << endl;
            }
        }
    }
};





class Notification {
private:
    string id;
    Recipient recipient;
    NotificationType type;
    string message;
    string subject;

    static string generateId() {
        static int counter = 0;
        return "notification_" + to_string(++counter);
    }

public:
    class Builder {
    private:
        Recipient recipient;
        NotificationType type;
        string message;
        string subject;

    public:
        Builder(const Recipient& recipient, NotificationType type)
            : recipient(recipient), type(type) {}

        Builder& setMessage(const string& message) {
            this->message = message;
            return *this;
        }

        Builder& setSubject(const string& subject) {
            this->subject = subject;
            return *this;
        }

        Notification build() {
            return Notification(*this);
        }

        friend class Notification;
    };

    Notification(const Builder& builder)
        : id(generateId()), recipient(builder.recipient), type(builder.type),
          message(builder.message), subject(builder.subject) {}

    string getId() const {
        return id;
    }

    Recipient getRecipient() const {
        return recipient;
    }

    NotificationType getType() const {
        return type;
    }

    string getMessage() const {
        return message;
    }

    string getSubject() const {
        return subject;
    }
};







class Recipient {
private:
    string userId;
    string email;
    string phoneNumber;
    string pushToken;
    bool hasEmail;
    bool hasPhoneNumber;
    bool hasPushToken;

public:
    Recipient(const string& userId, const string& email = "", const string& phoneNumber = "", const string& pushToken = "")
        : userId(userId), email(email), phoneNumber(phoneNumber), pushToken(pushToken),
          hasEmail(!email.empty()), hasPhoneNumber(!phoneNumber.empty()), hasPushToken(!pushToken.empty()) {}

    string getUserId() const {
        return userId;
    }

    string getEmail() const {
        return email;
    }

    bool hasEmailAddress() const {
        return hasEmail;
    }

    string getPhoneNumber() const {
        return phoneNumber;
    }

    bool hasPhoneNumberValue() const {
        return hasPhoneNumber;
    }

    string getPushToken() const {
        return pushToken;
    }

    bool hasPushTokenValue() const {
        return hasPushToken;
    }
};







enum class NotificationType {
    EMAIL,
    SMS,
    PUSH
};






class NotificationFactory {
private:
    static map<NotificationType, NotificationGateway*> gatewayMap;

public:
    static NotificationGateway* createGateway(NotificationType type) {
        auto it = gatewayMap.find(type);
        if (it != gatewayMap.end()) {
            return it->second;
        }

        NotificationGateway* gateway = NULL;

        switch (type) {
            case NotificationType::EMAIL:
                gateway = new EmailGateway();
                break;
            case NotificationType::SMS:
                gateway = new SmsGateway();
                break;
            case NotificationType::PUSH:
                gateway = new PushGateway();
                break;
        }

        gatewayMap[type] = gateway;
        return gateway;
    }
};

map<NotificationType, NotificationGateway*> NotificationFactory::gatewayMap;












class EmailGateway : public NotificationGateway {
public:
    void send(const Notification& notification) override {
        if (!notification.getRecipient().hasEmailAddress()) {
            throw runtime_error("Email address is required for EMAIL notification.");
        }

        cout << "--- Sending EMAIL ---" << endl;
        cout << "To: " << notification.getRecipient().getEmail() << endl;
        cout << "Subject: " << notification.getSubject() << endl;
        cout << "Body: " << notification.getMessage() << endl;
        cout << "---------------------" << endl << endl;
    }
};




class NotificationGateway {
public:
    virtual ~NotificationGateway() = default;
    virtual void send(const Notification& notification) = 0;
};








class PushGateway : public NotificationGateway {
public:
    void send(const Notification& notification) override {
        if (!notification.getRecipient().hasPushTokenValue()) {
            throw runtime_error("Push token is required for PUSH notification.");
        }

        cout << "--- Sending PUSH Notification ---" << endl;
        cout << "To Device Token: " << notification.getRecipient().getPushToken() << endl;
        cout << "Title: " << notification.getSubject() << endl;
        cout << "Body: " << notification.getMessage() << endl;
        cout << "---------------------------------" << endl << endl;
    }
};








class SmsGateway : public NotificationGateway {
public:
    void send(const Notification& notification) override {
        if (!notification.getRecipient().hasPhoneNumberValue()) {
            throw runtime_error("Phone number is required for SMS notification.");
        }

        cout << "--- Sending SMS ---" << endl;
        cout << "To: " << notification.getRecipient().getPhoneNumber() << endl;
        cout << "Message: " << notification.getMessage() << endl;
        cout << "-------------------" << endl << endl;
    }
};






class NotificationService {
private:
    TaskQueue taskQueue;

public:
    NotificationService(int poolSize) {
        // poolSize parameter kept for interface compatibility but not used
        cout << "NotificationService initialized with pool size: " << poolSize << endl;
    }

    void sendNotification(const Notification& notification) {
        taskQueue.enqueue([notification, this] {
            RetryableGatewayDecorator gateway(
                NotificationFactory::createGateway(notification.getType()),
                3
            );
            try {
                gateway.send(notification);
            } catch (const exception& e) {
                cout << "Exception while sending notification: " << e.what() << endl;
            }
        });
    }

    void processNotifications() {
        taskQueue.processAll();
    }

    void shutdown() {
        cout << "Processing remaining notifications..." << endl;
        processNotifications();
        cout << "NotificationService shutdown complete." << endl;
    }
};















int main() {
    // 1. Setup the notification service
    NotificationService notificationService(10);

    // 2. Define recipients
    Recipient recipient1("user123", "john.doe@example.com", "", "pushToken123");
    Recipient recipient2("user456", "", "+15551234567", "");

    // 3. Send various notifications using the Facade (NotificationService)

    // Scenario 1: Send a welcome email
    Notification welcomeEmail = Notification::Builder(recipient1, NotificationType::EMAIL)
            .setSubject("Welcome!")
            .setMessage("Welcome to notification system")
            .build();
    notificationService.sendNotification(welcomeEmail);

    // Scenario 2: Send a direct push notification
    Notification pushNotification = Notification::Builder(recipient1, NotificationType::PUSH)
            .setSubject("New Message")
            .setMessage("You have a new message from Jane.")
            .build();
    notificationService.sendNotification(puacshNotification);

    // Scenario 3: Send order confirmation SMS
    Notification orderSms = Notification::Builder(recipient2, NotificationType::SMS)
            .setMessage("Your order for Digital Clock is confirmed")
            .build();
    notificationService.sendNotification(orderSms);

    // Wait for a moment to allow the queue processor to work
    this_thread::sleep_for(chrono::milliseconds(1000));

    // 4. Shutdown the system
    cout << endl << "Shutting down the notification system..." << endl;
    notificationService.shutdown();
    cout << "System shut down successfully." << endl;

    return 0;
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































