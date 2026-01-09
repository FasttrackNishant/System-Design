package shoppingcartsystem;

public interface PricingStrategy {

    double calculatePrice(Item item,int quantity);
}
