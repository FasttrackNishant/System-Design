



class RetryableGatewayDecorator : INotificationGateway
{
    private readonly INotificationGateway wrappedGateway;
    private readonly int maxRetries;
    private readonly long retryDelayMillis;

    public RetryableGatewayDecorator(INotificationGateway wrappedGateway, int maxRetries, long retryDelayMillis)
    {
        this.wrappedGateway = wrappedGateway;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    public void Send(Notification notification)
    {
        int attempt = 0;
        while (attempt < maxRetries)
        {
            try
            {
                wrappedGateway.Send(notification);
                return; // Success
            }
            catch (Exception e)
            {
                attempt++;
                Console.WriteLine($"Error: Attempt {attempt} failed for notification {notification.GetId()}. Retrying...");
                if (attempt >= maxRetries)
                {
                    Console.WriteLine(e.Message);
                    throw new Exception($"Failed to send notification after {maxRetries} attempts.", e);
                }
                Thread.Sleep((int)retryDelayMillis);
            }
        }
    }
}







class Notification
{
    private readonly string id;
    private readonly Recipient recipient;
    private readonly NotificationType type;
    private readonly string message;
    private readonly string subject;

    private Notification(NotificationBuilder builder)
    {
        this.id = Guid.NewGuid().ToString();
        this.recipient = builder.recipient;
        this.type = builder.type;
        this.message = builder.message;
        this.subject = builder.subject;
    }

    public string GetId() { return id; }
    public Recipient GetRecipient() { return recipient; }
    public NotificationType GetType() { return type; }
    public string GetMessage() { return message; }
    public string GetSubject() { return subject; }
}

class NotificationBuilder
{
    internal readonly Recipient recipient;
    internal readonly NotificationType type;
    internal string message;
    internal string subject;

    public NotificationBuilder(Recipient recipient, NotificationType type)
    {
        this.recipient = recipient;
        this.type = type;
    }

    public NotificationBuilder Message(string message)
    {
        this.message = message;
        return this;
    }

    public NotificationBuilder Subject(string subject)
    {
        this.subject = subject;
        return this;
    }

    public Notification Build()
    {
        return new Notification(this);
    }
}













class Recipient
{
    private readonly string userId;
    private readonly string email;
    private readonly string phoneNumber;
    private readonly string pushToken;

    public Recipient(string userId, string email = null, string phoneNumber = null, string pushToken = null)
    {
        this.userId = userId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.pushToken = pushToken;
    }

    public string GetUserId()
    {
        return userId;
    }

    public string GetEmail()
    {
        return email;
    }

    public bool HasEmail()
    {
        return !string.IsNullOrEmpty(email);
    }

    public string GetPhoneNumber()
    {
        return phoneNumber;
    }

    public bool HasPhoneNumber()
    {
        return !string.IsNullOrEmpty(phoneNumber);
    }

    public string GetPushToken()
    {
        return pushToken;
    }

    public bool HasPushToken()
    {
        return !string.IsNullOrEmpty(pushToken);
    }
}














enum NotificationType
{
    EMAIL,
    SMS,
    PUSH
}






class NotificationFactory
{
    private static readonly Dictionary<NotificationType, INotificationGateway> gatewayMap = 
        new Dictionary<NotificationType, INotificationGateway>();

    public static INotificationGateway CreateGateway(NotificationType type)
    {
        if (gatewayMap.ContainsKey(type))
        {
            return gatewayMap[type];
        }

        INotificationGateway gateway = null;

        switch (type)
        {
            case NotificationType.EMAIL:
                gateway = new EmailGateway();
                break;
            case NotificationType.SMS:
                gateway = new SmsGateway();
                break;
            case NotificationType.PUSH:
                gateway = new PushGateway();
                break;
        }

        gatewayMap[type] = gateway;
        return gateway;
    }
}








class EmailGateway : INotificationGateway
{
    public void Send(Notification notification)
    {
        if (!notification.GetRecipient().HasEmail())
        {
            throw new ArgumentException("Email address is required for EMAIL notification.");
        }

        string email = notification.GetRecipient().GetEmail();
        Console.WriteLine("--- Sending EMAIL ---");
        Console.WriteLine($"To: {email}");
        Console.WriteLine($"Subject: {notification.GetSubject()}");
        Console.WriteLine($"Body: {notification.GetMessage()}");
        Console.WriteLine("---------------------\n");
    }
}








interface INotificationGateway
{
    void Send(Notification notification);
}








class PushGateway : INotificationGateway
{
    public void Send(Notification notification)
    {
        if (!notification.GetRecipient().HasPushToken())
        {
            throw new ArgumentException("Push token is required for PUSH notification.");
        }

        string token = notification.GetRecipient().GetPushToken();
        Console.WriteLine("--- Sending PUSH Notification ---");
        Console.WriteLine($"To Device Token: {token}");
        Console.WriteLine($"Title: {notification.GetSubject()}");
        Console.WriteLine($"Body: {notification.GetMessage()}");
        Console.WriteLine("---------------------------------\n");
    }
}









class SmsGateway : INotificationGateway
{
    public void Send(Notification notification)
    {
        if (!notification.GetRecipient().HasPhoneNumber())
        {
            throw new ArgumentException("Phone number is required for SMS notification.");
        }

        string phone = notification.GetRecipient().GetPhoneNumber();
        Console.WriteLine("--- Sending SMS ---");
        Console.WriteLine($"To: {phone}");
        Console.WriteLine($"Message: {notification.GetMessage()}");
        Console.WriteLine("-------------------\n");
    }
}

















class NotificationService
{
    private readonly TaskScheduler scheduler;
    private readonly CancellationTokenSource cancellationTokenSource;

    public NotificationService(int poolSize)
    {
        var factory = new TaskFactory(
            new LimitedConcurrencyLevelTaskScheduler(poolSize));
        this.scheduler = factory.Scheduler;
        this.cancellationTokenSource = new CancellationTokenSource();
    }

    public void SendNotification(Notification notification)
    {
        Task.Factory.StartNew(() =>
        {
            INotificationGateway gateway = new RetryableGatewayDecorator(
                NotificationFactory.CreateGateway(notification.GetType()),
                3,
                1000);
            try
            {
                gateway.Send(notification);
            }
            catch (Exception e)
            {
                Console.WriteLine($"Exception while sending notification: {e}");
            }
        }, cancellationTokenSource.Token, TaskCreationOptions.None, scheduler);
    }

    public void Shutdown()
    {
        cancellationTokenSource.Cancel();
    }
}









using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Concurrent;

public class NotificationSystemDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the notification service
        NotificationService notificationService = new NotificationService(10);

        // 2. Define recipients
        Recipient recipient1 = new Recipient("user123", "john.doe@example.com", null, "pushToken123");
        Recipient recipient2 = new Recipient("user456", null, "+15551234567", null);

        // 3. Send various notifications using the Facade (NotificationService)

        // Scenario 1: Send a welcome email
        Notification welcomeEmail = new NotificationBuilder(recipient1, NotificationType.EMAIL)
                .Subject("Welcome!")
                .Message("Welcome to notification system")
                .Build();
        notificationService.SendNotification(welcomeEmail);

        // Scenario 2: Send a direct push notification
        Notification pushNotification = new NotificationBuilder(recipient1, NotificationType.PUSH)
                .Subject("New Message")
                .Message("You have a new message from Jane.")
                .Build();
        notificationService.SendNotification(pushNotification);

        // Scenario 3: Send order confirmation SMS
        Notification orderSms = new NotificationBuilder(recipient2, NotificationType.SMS)
                .Message("Your order for Digital Clock is confirmed")
                .Build();
        notificationService.SendNotification(orderSms);

        // Wait for a moment to allow the queue processor to work
        Thread.Sleep(1000);

        // 4. Shutdown the system
        Console.WriteLine("\nShutting down the notification system...");
        notificationService.Shutdown();
        Console.WriteLine("System shut down successfully.");
    }
}



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































