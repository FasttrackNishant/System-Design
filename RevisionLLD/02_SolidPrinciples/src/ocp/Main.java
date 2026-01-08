package ocp;

public class Main {

    public static void main(String[] args) {

        PaymentMethod creditCard = new CreditCardPaymentProcessor();
        CheckoutService checkoutService = new CheckoutService();
        checkoutService.processPaymnet(creditCard,3);


    }
}
