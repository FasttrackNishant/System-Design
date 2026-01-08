package ocp;

public class CheckoutService {

    public  void processPaymnet(PaymentMethod paymentMethod , int amount){

        PaymentProcessor paymentProcessor = new PaymentProcessor();
        paymentProcessor.processPayment(amount);

    }

}
