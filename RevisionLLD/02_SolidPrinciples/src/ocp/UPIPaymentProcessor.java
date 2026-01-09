package ocp;

public class UPIPaymentProcessor implements PaymentMethod{

    @Override
    public void processPayment(int amount)
    {
        System.out.println("Payment Proceeded with the UP " + amount);
    }

}
