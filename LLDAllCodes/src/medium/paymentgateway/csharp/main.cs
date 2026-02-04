enum PaymentMethod
{
    CREDIT_CARD,
    PAYPAL,
    UPI
}





enum PaymentStatus
{
    INITIATED,
    SUCCESSFUL,
    FAILED
}





class PaymentProcessorFactory
{
    public static IPaymentProcessor GetProcessor(PaymentMethod method)
    {
        switch (method)
        {
            case PaymentMethod.CREDIT_CARD:
                return new CreditCardProcessor();
            case PaymentMethod.UPI:
                return new UPIProcessor();
            case PaymentMethod.PAYPAL:
                return new PayPalProcessor();
            default:
                throw new ArgumentException($"Unsupported payment method: {method}");
        }
    }
}






class PaymentRequest
{
    private readonly string transactionId;
    private readonly string payerId;
    private readonly double amount;
    private readonly string currency;
    private readonly PaymentMethod paymentMethod;
    private readonly Dictionary<string, string> paymentDetails;

    public PaymentRequest(PaymentRequestBuilder builder)
    {
        this.transactionId = Guid.NewGuid().ToString();
        this.payerId = builder.payerId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.paymentMethod = builder.paymentMethod;
        this.paymentDetails = builder.paymentDetails;
    }

    public string GetTransactionId() { return transactionId; }
    public double GetAmount() { return amount; }
    public string GetCurrency() { return currency; }
    public PaymentMethod GetPaymentMethod() { return paymentMethod; }
}

class PaymentRequestBuilder
{
    internal string payerId;
    internal double amount;
    internal string currency;
    internal PaymentMethod paymentMethod;
    internal Dictionary<string, string> paymentDetails;

    public PaymentRequestBuilder PayerId(string payerId)
    {
        this.payerId = payerId;
        return this;
    }

    public PaymentRequestBuilder Amount(double amount)
    {
        this.amount = amount;
        return this;
    }

    public PaymentRequestBuilder Currency(string currency)
    {
        this.currency = currency;
        return this;
    }

    public PaymentRequestBuilder PaymentMethod(PaymentMethod paymentMethod)
    {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentRequestBuilder PaymentDetails(Dictionary<string, string> paymentDetails)
    {
        this.paymentDetails = paymentDetails;
        return this;
    }

    public PaymentRequest Build()
    {
        return new PaymentRequest(this);
    }
}





class PaymentResponse
{
    private readonly PaymentStatus status;
    private readonly string message;

    public PaymentResponse(PaymentStatus status, string message)
    {
        this.status = status;
        this.message = message;
    }

    public PaymentStatus GetStatus() { return status; }
    public string GetMessage() { return message; }
}





class Transaction
{
    private readonly string id;
    private readonly PaymentRequest request;
    private PaymentStatus status;
    private readonly DateTime timestamp;

    public Transaction(PaymentRequest request)
    {
        this.id = request.GetTransactionId();
        this.request = request;
        this.status = PaymentStatus.INITIATED;
        this.timestamp = DateTime.Now;
    }

    public void SetStatus(PaymentStatus status) { this.status = status; }

    public string GetId() { return id; }
    public PaymentStatus GetStatus() { return status; }
    public PaymentRequest GetRequest() { return request; }
}









class CustomerNotifier : IPaymentObserver
{
    public void OnTransactionUpdate(Transaction transaction)
    {
        if (transaction.GetStatus() == PaymentStatus.SUCCESSFUL)
        {
            Console.WriteLine("--- CUSTOMER EMAIL ---");
            Console.WriteLine($"Your payment of {transaction.GetRequest().GetAmount()} was successful. Transaction ID: {transaction.GetId()}");
            Console.WriteLine("----------------------");
        }
    }
}




interface IPaymentObserver
{
    void OnTransactionUpdate(Transaction transaction);
}






class MerchantNotifier : IPaymentObserver
{
    public void OnTransactionUpdate(Transaction transaction)
    {
        Console.WriteLine("--- MERCHANT NOTIFICATION ---");
        Console.WriteLine($"Transaction {transaction.GetId()} status updated to: {transaction.GetStatus()}");
        Console.WriteLine("-----------------------------");
    }
}







abstract class AbstractPaymentProcessor : IPaymentProcessor
{
    private const int MAX_RETRIES = 3;

    public PaymentResponse ProcessPayment(PaymentRequest request)
    {
        int attempts = 0;
        PaymentResponse response;
        do
        {
            response = DoProcess(request);
            attempts++;
        } while (response.GetStatus() == PaymentStatus.FAILED && attempts < MAX_RETRIES);
        
        return response;
    }

    protected abstract PaymentResponse DoProcess(PaymentRequest request);
}



class CreditCardProcessor : AbstractPaymentProcessor
{
    protected override PaymentResponse DoProcess(PaymentRequest request)
    {
        Console.WriteLine($"Processing credit card payment of amount {request.GetAmount()} {request.GetCurrency()}");
        // Simulate interaction with Visa/Mastercard network
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "Credit Card payment successful.");
    }
}




interface IPaymentProcessor
{
    PaymentResponse ProcessPayment(PaymentRequest request);
}




class PayPalProcessor : AbstractPaymentProcessor
{
    protected override PaymentResponse DoProcess(PaymentRequest request)
    {
        Console.WriteLine($"Redirecting to PayPal for transaction {request.GetTransactionId()}");
        // Simulate PayPal API interaction
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "Paypal payment successful.");
    }
}


class UPIProcessor : AbstractPaymentProcessor
{
    protected override PaymentResponse DoProcess(PaymentRequest request)
    {
        Console.WriteLine($"Processing UPI payment of {request.GetAmount()} {request.GetCurrency()}");
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "UPI payment successful.");
    }
}








using System;
using System.Collections.Generic;

public class PaymentGatewayDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the gateway facade
        PaymentGatewayService paymentGateway = PaymentGatewayService.GetInstance();

        // 2. Register observers to be notified of transaction events
        paymentGateway.AddObserver(new MerchantNotifier());
        paymentGateway.AddObserver(new CustomerNotifier());

        Console.WriteLine("----------- SCENARIO 1: Successful Credit Card Payment -----------");
        // a. Merchant's backend creates a payment request
        PaymentRequest ccRequest = new PaymentRequestBuilder()
                .PayerId("U-123")
                .Amount(150.75)
                .Currency("INR")
                .PaymentMethod(PaymentMethod.CREDIT_CARD)
                .PaymentDetails(new Dictionary<string, string> { { "cardNumber", "1234..." } })
                .Build();

        // b. Merchant's backend sends it to the facade
        paymentGateway.ProcessPayment(ccRequest);

        Console.WriteLine("\n----------- SCENARIO 2: Successful PayPal Payment -----------");
        PaymentRequest paypalRequest = new PaymentRequestBuilder()
                .PayerId("U-456")
                .Amount(88.50)
                .Currency("USD")
                .PaymentMethod(PaymentMethod.PAYPAL)
                .PaymentDetails(new Dictionary<string, string> { { "email", "customer@example.com" } })
                .Build();

        paymentGateway.ProcessPayment(paypalRequest);
    }
}










class PaymentGatewayService
{
    private static PaymentGatewayService instance;
    private readonly List<IPaymentObserver> observers = new List<IPaymentObserver>();

    private PaymentGatewayService() { }

    public static PaymentGatewayService GetInstance()
    {
        if (instance == null)
        {
            instance = new PaymentGatewayService();
        }
        return instance;
    }

    public void AddObserver(IPaymentObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IPaymentObserver observer)
    {
        observers.Remove(observer);
    }

    private void NotifyObservers(Transaction transaction)
    {
        foreach (var observer in observers)
        {
            observer.OnTransactionUpdate(transaction);
        }
    }

    public Transaction ProcessPayment(PaymentRequest request)
    {
        Transaction transaction = new Transaction(request);
        try
        {
            IPaymentProcessor processor = PaymentProcessorFactory.GetProcessor(request.GetPaymentMethod());
            PaymentResponse response = processor.ProcessPayment(request);
            transaction.SetStatus(response.GetStatus());
        }
        catch (Exception e)
        {
            Console.Error.WriteLine($"Payment processing failed: {e.Message}");
            transaction.SetStatus(PaymentStatus.FAILED);
        }
        
        NotifyObservers(transaction);
        return transaction;
    }
}



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































