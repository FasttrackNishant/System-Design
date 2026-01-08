package cardmasker;

public class PaymentProcessor {

    public void process(PaymentMethod paymentMethod, double amount) {
        validateAmount(amount);

        // actual payment logic / gateway call here
        System.out.println(
                "Processing payment of " + amount +
                        " using " + paymentMethod.getMaskedIdentifier()
        );
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new InvalidPaymentDetailsException("Amount must be greater than zero");
        }
    }
}
