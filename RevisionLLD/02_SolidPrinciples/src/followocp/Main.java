package followocp;

public class Main {

    public static void main(String[] args) {
        CheckoutService checkout = new CheckoutService();
        checkout.processPayment(new CreditCardPayment(), 100.00);
        checkout.processPayment(new PayPalPayment(), 100.00);
    }
}
