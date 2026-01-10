package shoppingcartsystem;

public class DiscountStrategy implements  PricingStrategy{

    private double discount;

    public DiscountStrategy(double discount){
        this.discount = discount;
    }

    @Override
    public double calculatePrice(Item item , int quantity)
    {
        double base = item.getPrice()* quantity;
        return base - (base*discount/100);
    }
}
