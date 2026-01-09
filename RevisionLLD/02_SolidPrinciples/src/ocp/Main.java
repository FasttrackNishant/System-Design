package ocp;

public class Main {

    public static void main(String[] args) {

        PaymentMethod creditCard = new CreditCardPaymentProcessor();
        PaymentMethod upiPayment = new UPIPaymentProcessor();
        CheckoutService checkoutService = new CheckoutService();
        checkoutService.processPayment(upiPayment,3);


    }
}
