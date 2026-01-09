package followocp;

class CheckoutService {
    public void processPayment(PaymentMethod method, double amount) {
        PaymentProcessor processor = new PaymentProcessor();
        processor.process(method, amount);
    }
}

