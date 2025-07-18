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

    private double totalPrice() {
        double total = 0;
        for (int i = 0; i < prices.size(); i++) {
            total += quantities.get(i) * prices.get(i);
        }
        return total;
    }

    public void pay(String paymentType, String securityCode) {
        if ("debit".equals(paymentType)) {
            System.out.println("Processing debit payment type");
            System.out.println("Verifying security code: " + securityCode);
            status = "paid";
        } else if ("credit".equals(paymentType)) {
            System.out.println("Processing credit payment type");
            System.out.println("Verifying security code: " + securityCode);
            status = "paid";
        } else {
            throw new RuntimeException("Unknown payment type: " + paymentType);
        }
    }  

    public static void main(String[] args) {
        Order order = new Order();
        order.addItem("Keyboard", 1, 50);
        order.addItem("SSD", 1, 150);
        order.addItem("USB cable", 2, 5);

        System.out.println(order.totalPrice());
        order.pay("debit", "0372846");
    }
}
