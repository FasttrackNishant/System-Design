package medium.pizzashop.core;

import java.util.ArrayList;
import java.util.List;

enum PizzaType {
    MARGHERITA(200),
    FARMHOUSE(250);

    private double price;

    PizzaType(double price) {
        this.price = price;
    }

    public double getPrice() {
        return this.price;
    }
}

enum ToppingType {
    CHEESE(20),
    OLIVE(30),
    MUSHROOM(40);

    private final double price;

    ToppingType(double price) {
        this.price = price;
    }

    public double getPrice() {
        return this.price;
    }
}


interface Pizza {
    String getDescription();

    double getPrice();
}

abstract class BasePizza implements Pizza {

    protected final PizzaType pizzaType;

    public BasePizza(PizzaType pizzaType) {
        this.pizzaType = pizzaType;
    }

    @Override
    public double getPrice() {
        return pizzaType.getPrice();
    }

    @Override
    public String getDescription() {
        return pizzaType.name();
    }
}

class Margherita extends BasePizza {

    public Margherita() {
        super(PizzaType.MARGHERITA);
    }
}

class FARMHOUSE extends BasePizza {

    public FARMHOUSE() {
        super(PizzaType.FARMHOUSE);
    }
}

abstract class ToppingDecorater implements Pizza {

    protected final Pizza pizza;
    protected final ToppingType toppingType;

    protected ToppingDecorater(Pizza pizza, ToppingType toppingType) {
        this.pizza = pizza;
        this.toppingType = toppingType;
    }

    @Override
    public String getDescription() {
        return pizza.getDescription() + toppingType.name();
    }

    @Override
    public double getPrice() {
        return pizza.getPrice() + toppingType.getPrice();
    }
}

class CheeseTopping extends ToppingDecorater {

    public CheeseTopping(Pizza pizza) {
        super(pizza, ToppingType.CHEESE);
    }
}

class MushroomTopping extends ToppingDecorater {
    public MushroomTopping(Pizza pizza) {
        super(pizza, ToppingType.MUSHROOM);
    }
}

class OliveTopping extends ToppingDecorater {
    public OliveTopping(Pizza pizza) {
        super(pizza, ToppingType.OLIVE);
    }
}


class Order {

    private List<Pizza> pizzaList = new ArrayList<>();

    public void addPizza(Pizza pizza) {
        pizzaList.add(pizza);
    }

    public double getTotalCost() {
        double totalCost = 0;

        for (Pizza pizza : pizzaList) {
            totalCost += pizza.getPrice();
        }

        return totalCost;
    }

    public void printOrder() {
        System.out.println("Your order summary");

        for (Pizza pizza : pizzaList) {
            System.out.println(pizza.getDescription() + " => " + pizza.getPrice());
        }
    }
}

class PizzaOrderingShop {

    public static void main(String[] args) {

        Pizza pizza1 = new Margherita();

        pizza1 = new CheeseTopping(pizza1);
        pizza1 = new OliveTopping(pizza1);

        Pizza pizza2 = new FARMHOUSE();
        pizza2 = new CheeseTopping(pizza2);

        Order order = new Order();
        order.addPizza(pizza1);
        order.addPizza(pizza2);

        order.printOrder();

    }
}