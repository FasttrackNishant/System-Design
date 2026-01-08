package ocp;

public class PaymentProcessor {

    public void  processPayment(PaymentMethod paymentMethod,int amount){
        paymentMethod.processPayment(amount);
    }

}
