package behavioural.strategy.solution;

class FlatRateShipping implements ShippingStrategy {
    private double rate;

    public FlatRateShipping(double rate) {
        this.rate = rate;
    }

    @Override
    public double calculateCost(Order order) {
        System.out.println("Calculating with Flat Rate behavioural.strategy ($" + rate + ")");
        return rate;
    }
}