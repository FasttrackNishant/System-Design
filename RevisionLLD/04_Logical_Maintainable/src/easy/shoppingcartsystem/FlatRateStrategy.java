package easy.shoppingcartsystem;

public class FlatRateStrategy implements  PricingStrategy{

    @Override
    public double calculatePrice(Item item,int quantity){
        return  item.getPrice()*quantity;
    }
}
