package strategy.violation;

public class Main {

    public static void main(String[] args){

        Order order1 = new Order();
        ShippingCostCalculator calculator = new ShippingCostCalculator();
        double cost = calculator.calculateCost(order1,OrderFeeType.WEIGHTBASED);

        System.out.println("My Cost is "+ cost);

    }
}
