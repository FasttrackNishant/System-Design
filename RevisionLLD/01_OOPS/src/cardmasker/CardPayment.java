package cardmasker;

public class CardPayment implements PaymentMethod {

    private final String cardNumber;

    public CardPayment(String cardNumber) {
        validate(cardNumber);
        this.cardNumber = cardNumber;
    }

    @Override
    public String getMaskedIdentifier() {
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    private void validate(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 12) {
            throw new InvalidPaymentDetailsException("Invalid card number");
        }
    }
}
