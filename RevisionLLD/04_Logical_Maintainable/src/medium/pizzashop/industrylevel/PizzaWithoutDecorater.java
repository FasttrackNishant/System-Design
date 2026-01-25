package medium.pizzashop.industrylevel;


import java.util.ArrayList;
import java.util.List;

enum PizzaBase {

    MAG(100),
    FARM(200);

    private final double price;

    PizzaBase(double price) {
        this.price = price;
    }

    public double getPrice() {
        return this.price;
    }
}

enum ToppingType {

    CHEESE(20),
    OLIVES(30);

    private final double price;

    ToppingType(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}

enum PizzaSize {

    SMALL(1.2),
    MEDIUM(1.5),
    LARGE(2.0);

    private final double price;

    PizzaSize(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}

class Pizza {

    private PizzaBase base;
    private List<ToppingType> toppings = new ArrayList<>();
    private PizzaSize size;

    public Pizza(PizzaSize size, PizzaBase base, List<ToppingType> toppings) {
        this.size = size;
        this.base = base;
        this.toppings = toppings;
    }

    public PizzaBase getBase() {
        return base;
    }

    public PizzaSize getSize() {
        return size;
    }

    public List<ToppingType> getToppings() {
        return toppings;
    }

    public void printDetails(){

        System.out.println("Base : " + base);
        System.out.println("Size : "+ size );

        System.out.print("Toppings");

        for(ToppingType type : toppings){
            System.out.println(type.name());
        }

    }

}

class PricingEngine {

    public double calculatePrice(Pizza pizza) {
        double totalCost = pizza.getBase().getPrice();

        for(ToppingType topping : pizza.getToppings()){
            totalCost += topping.getPrice();
        }

        totalCost *= pizza.getSize().getPrice();

        return  totalCost;
    }
}

class BillingService {

    private PricingEngine engine;

    public BillingService(PricingEngine engine){
        this.engine = engine;
    }

    public double calculateCost(Pizza pizza){
        return engine.calculatePrice(pizza);
    }

    public void printOrder(Pizza pizza){
        pizza.printDetails();
    }

}

class PizzaShop{

    public static void main(String[] args) {


        List<ToppingType> toppings= List.of(ToppingType.CHEESE,ToppingType.OLIVES);
        Pizza pizza = new Pizza(PizzaSize.MEDIUM, PizzaBase.MAG,toppings);


        BillingService billingService = new BillingService(new PricingEngine());

        billingService.calculateCost(pizza);

        billingService.printOrder(pizza);

    }
}


