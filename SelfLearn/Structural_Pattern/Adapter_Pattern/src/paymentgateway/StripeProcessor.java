package paymentgateway;

public class StripeProcessor implements PaymentProcessor {

    private  String transactionId ;
    private boolean isPaymentSuccess;

    @Override
    public void processPayment(double amount, String currency) {
        System.out.println("Stripe Payment Successfully");
        this.isPaymentSuccess = true;
        this.transactionId = "TXN"+System.currentTimeMillis();
    }

    @Override
    public boolean getPaymentStatus() {
        return isPaymentSuccess;
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }
}
