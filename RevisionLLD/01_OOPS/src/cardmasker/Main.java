package cardmasker;

public class Main {
    public static void main(String[] args) {

        PaymentMethod cardPayment =
                new CardPayment("1234567812345678");

        PaymentProcessor processor = new PaymentProcessor();
        processor.process(cardPayment, 250.00);
    }
}
