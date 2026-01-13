package easy.shoppingcartsystem;

public class CartItem  {

    private final Item item;
    private int quantity;
    private PricingStrategy pricingStrategy;

    public CartItem(Item item , int quantity){
        this.item = item;
        this.quantity = quantity;
        this.pricingStrategy = new FlatRateStrategy();
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "item=" + item +
                ", quantity=" + quantity +
                '}';
    }

    public void increaseQuantity(int quantity){

        if(quantity < 0){
            throw new IllegalArgumentException("Quantity can't be negative");
        }

        this.quantity += quantity;
    }

    public void setPricingStrategy(PricingStrategy strategy){
        this.pricingStrategy = strategy;
    }

    public double getPrice(){
        return pricingStrategy.calculatePrice(this.item,this.quantity);
    }
}
