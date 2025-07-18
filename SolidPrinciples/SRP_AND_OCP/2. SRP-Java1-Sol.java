import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<String> items;
    private List<Integer> quantities;
    private List<Double> prices;
    private String status;

    public Order() {
        this.items = new ArrayList<>();
        this.quantities = new ArrayList<>();
        this.prices = new ArrayList<>();
        this.status = "open";
    }

    public void addItem(String name, int quantity, double price) {
        items.add(name);
        quantities.add(quantity);
        prices.add(price);
    }

    public double totalPrice() {
        double total = 0;
        for (int i = 0; i < prices.size(); i++) {
            total += quantities.get(i) * prices.get(i);
        }
        return total;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}

/**
 * 2. SRP-Java1-Sol
 */
interface PaymentProcessor {
    void pay(Order order, String securityCode);
}

class DebitPaymentProcessor implements PaymentProcessor {
    public void pay(Order order, String securityCode) {
        System.out.println("Processing debit payment type");
        System.out.println("Verifying security code: " + securityCode);
        order.setStatus("paid");
    }
}

class CreditPaymentProcessor implements PaymentProcessor {
    public void pay(Order order, String securityCode) {
        System.out.println("Processing credit payment type");
        System.out.println("Verifying security code: " + securityCode);
        order.setStatus("paid");
    }
}

class UPIPaymentProcessor implements PaymentProcessor {
    public void pay(Order order, String securityCode) {
        System.out.println("Processing UPI payment type");
        System.out.println("Verifying security code: " + securityCode);
        order.setStatus("paid");
    }
}

// class PaymentProcessor {
// public void pay(Order order, String securityCode, String paymentType) {
// if ("debit".equals(paymentType)) {
// System.out.println("Processing debit payment type");
// System.out.println("Verifying security code: " + securityCode);
// order.setStatus("paid");
// } else if ("credit".equals(paymentType)) {
// System.out.println("Processing credit payment type");
// System.out.println("Verifying nkjfnks security code: " + securityCode);
// order.setStatus("paid");
// } else if ("UPI".equals(paymentType)) {
// System.out.println("Processing UPI payment type");
// System.out.println("Verifying security code: " + securityCode);
// order.setStatus("paid");
// }
// }
// }

class Main {
    public static void main(String[] args) {
        Order order = new Order();
        order.addItem("Keyboard", 1, 50);
        order.addItem("SSD", 1, 150);
        order.addItem("USB cable", 2, 5);

        System.out.println(order.totalPrice());

        // PaymentProcessor processor = new PaymentProcessor();
        // processor.pay(order, "0372846", "debit");

        PaymentProcessor processor = new DebitPaymentProcessor();
        processor.pay(order, "y3743y7");
    }
}
