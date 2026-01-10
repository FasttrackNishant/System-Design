package shoppingcartsystem;

import java.util.HashMap;
import java.util.Map;

public class Cart {

    private final String cartId;
    private  Map<String,CartItem> cartItems;

    public Cart(String cartId){
        this.cartId = cartId;
        this.cartItems = new HashMap<>();
    }

    public Map<String,CartItem> getCart(){
       return  this.cartItems;
    }

    public boolean addItemQuantity(String itemId , int quantity){

        if(quantity < 0) {
            System.out.println("Quantity is negative Operatino failed");
            return false;
        }

        CartItem tobeUpdated = cartItems.get(itemId);

        if(tobeUpdated == null){
            System.out.println("Item no present in the system");
            return false;
        }

        tobeUpdated.increaseQuantity(quantity);
        return true;

    }

    public void addToCart(Item itemToAdd,int quantity){

        if(itemToAdd == null){
            return;
        }

        // First fetch itemid
        String itemId = itemToAdd.getItemId();

        if(cartItems.containsKey(itemId)){

            CartItem existingItem = cartItems.get(itemId);
            existingItem.increaseQuantity(quantity);

        }
        else
        {
            CartItem newCartItem = new CartItem(itemToAdd,quantity);
            cartItems.put(itemId,newCartItem);
        }
    }

    public double getTotalCartPrice(){
        double totalPrice = 0;

        for(CartItem item : cartItems.values())
        {
            totalPrice += item.getPrice();
        }

        return  totalPrice;
    }

    public void applyPricingStrategy(PricingStrategy strategy, String itemId){

        CartItem cartItem = cartItems.get(itemId);
        cartItem.setPricingStrategy(strategy);
    }


}
