package paymentgateway;

public class CheckoutService {

    private PaymentProcessor paymentProcessor;

    public CheckoutService(PaymentProcessor paymentProcessor)
    {
        this.paymentProcessor = paymentProcessor;
    }

    public void checkout(double amount,String currency)
    {
        paymentProcessor.processPayment(amount,currency);
        boolean isSuccess = paymentProcessor.getPaymentStatus();

        if(isSuccess)
        {
            System.out.println("Payment Success");
        }
        else
        {
            System.out.println("Payment failed");
        }
    }
}
