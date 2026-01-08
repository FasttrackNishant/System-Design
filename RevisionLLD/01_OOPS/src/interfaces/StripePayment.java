package interfaces;

public class StripePayment implements PaymentGateway{

    @Override
    public void initiatePayment(double amount){
        System.out.println("Stripe Payment Initiated" + amount);
    }
}
