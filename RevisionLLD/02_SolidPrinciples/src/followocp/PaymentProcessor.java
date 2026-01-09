package followocp;

class PaymentProcessor {
    public void process(PaymentMethod paymentMethod, double amount) {
        // No more if-else! The processor doesn't care about the specific type.
        // It just knows it can call processPayment.
        paymentMethod.processPayment(amount);
    }
}