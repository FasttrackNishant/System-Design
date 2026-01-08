package ocp;

public class CreditCardPaymentProcessor implements PaymentMethod{

    @Override
    public void processPayment(int amount){
        System.out.println("Actual Payment Processed by credit card");
    }
}
