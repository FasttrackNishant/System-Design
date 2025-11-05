package paymentgateway;

public interface PaymentProcessor {

    boolean getPaymentStatus();

    void processPayment(double amount , String currency);

    String getTransactionId();
}
