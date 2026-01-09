package strategy.solution;

public class Order {

    private int orderId;
    private double weight;
    private double distance;

    public Order(){
        this.orderId = 1;
        this.weight = 30;
        this.distance = 500;
    }

    public double getTotalWeight()
    {
        return this.weight;
    }

    public double getDistance(){
        return this.distance;
    }
}
