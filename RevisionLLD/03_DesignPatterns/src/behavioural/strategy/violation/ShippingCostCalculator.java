package behavioural.strategy.violation;

public class ShippingCostCalculator{

    public double cost = 0.0;

    public double calculateCost(Order order , OrderFeeType type){

        if(type.equals(OrderFeeType.FLATBASED))
        {
            System.out.println("Calculating cost on the flat basis");
            cost = 1000;
        }
        else if(type.equals(OrderFeeType.WEIGHTBASED))
        {
            System.out.println("Calculating cost on the weight basis");
            cost = order.getWeight() * 100;
        }
        else if((type.equals(OrderFeeType.DISTANCEBASED)))
        {
            cost = order.getDistance() *  50;
        }

        return cost;

    }
}
