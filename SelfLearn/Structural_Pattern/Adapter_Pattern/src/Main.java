import paymentgateway.*;

public class Main {
    public static void main(String[] args) {

        //Client


//        PaymentProcessor paymentProcessor = new StripeProcessor();

        LegacyPaymentGateWay legacyPaymentGateWay = new LegacyPaymentGateWay();
        PaymentProcessor paymentProcessor = new LegacyPaymentAdapter(legacyPaymentGateWay);
        CheckoutService checkout = new CheckoutService(paymentProcessor);
        checkout.checkout(23,"INR");

    }
}