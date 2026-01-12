package behavioural.strategy.solution;

public class Main {
    public static void main(String[] args) {

        Order order1 = new Order();

        // Create different behavioural.strategy instances
        ShippingStrategy flatRate = new FlatRateShipping(10.0);
        ShippingStrategy weightBased = new WeightBasedShipping(5);

        // Create context with an initial behavioural.strategy
        ShippingCostService shippingService = new ShippingCostService(flatRate);

        System.out.println("--- Order 1: Using Flat Rate (initial) ---");
        shippingService.calculateShippingCost(order1);

        System.out.println("\n--- Order 1: Changing to Weight-Based ---");
        shippingService.setStrategy(weightBased);
        shippingService.calculateShippingCost(order1);
    }
}
