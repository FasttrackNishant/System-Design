import enums.Coin;
import enums.OrderStatus;
import interfaces.PaymentGateway;
import interfaces.RazorPayPayment;
import interfaces.StripePayment;
import models.Car;
import service.CheckoutService;

public class App {
    public static void main(String[] args) throws Exception {

        System.out.println("Hello, World!");


        // Class Object
        Car mercedes = new Car("BMW","m1",400);

        mercedes.displayStatus();
        mercedes.accelerate(44);
        mercedes.displayStatus();


        // Enums
        OrderStatus orderStatus = OrderStatus.PLACED;
        System.out.println(orderStatus);

        // Enums
        int coinValue = Coin.HUND.getValue();
        System.out.println(coinValue);

        // Interface
        PaymentGateway stripePaymentGateway = new StripePayment();
        PaymentGateway razorpayPaymentGateway = new RazorPayPayment();
        CheckoutService checkoutService = new CheckoutService(razorpayPaymentGateway);

        checkoutService.checkout(45.89);

    }
}
