package ocp;

public class CheckoutService {

    public  void processPayment(PaymentMethod paymentMethod , int amount){

        PaymentProcessor paymentProcessor = new PaymentProcessor();
        paymentProcessor.processPayment(paymentMethod,amount);

    }

}
