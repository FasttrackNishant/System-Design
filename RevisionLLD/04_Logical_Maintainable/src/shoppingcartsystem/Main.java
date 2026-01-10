package shoppingcartsystem;

public class Main {

    public static void main(String[] args) {

        Item parleg = new Item("1","ParLe G ",10);
        Item crackjack = new Item("2","Crack Jack",20);

        Cart myCart = new Cart("101");
        PricingStrategy discount = new DiscountStrategy(48);

        myCart.addToCart(parleg,2);
        myCart.addToCart(crackjack,10);

        myCart.applyPricingStrategy(discount,crackjack.getItemId());

        for(CartItem item : myCart.getCart().values()){
            System.out.println(item);
        }

        double totalPrice = myCart.getTotalCartPrice();

        System.out.println(totalPrice);

    }
}
