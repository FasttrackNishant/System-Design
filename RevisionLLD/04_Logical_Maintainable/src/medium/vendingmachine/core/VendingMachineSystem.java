package medium.vendingmachine.core;

import java.util.HashMap;
import java.util.Map;

enum Coin {
    FIVE(5),
    TEN(10),
    TWENTY(20);

    private double value;

    Coin(double value) {
        this.value = value;
    }

    public double getCoinValue() {
        return value;
    }
}

class Item {

    private String name;
    private String code;
    private double price;

    public Item(String code, String name, int price) {
        this.code = code;
        this.name = name;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

class Inventory {
    private Map<String, Item> itemMap = new HashMap<>();
    private Map<String, Integer> stockMap = new HashMap<>();

    public void addItem(String code, Item item, int quantity) {
        itemMap.put(code, item);
        stockMap.put(code, quantity);
    }

    public void reduceStock(String code) {
        stockMap.put(code, stockMap.get(code) - 1);
    }

    public boolean isAvailable(String code) {
        return stockMap.getOrDefault(code, 0) > 0;
    }

    public Item getItem(String code) {
        return itemMap.get(code);
    }
}

abstract class VendingMachineState {

    protected VendingMachine vendingMachine;

    VendingMachineState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    public abstract void insertCoin(Coin coin);

    public abstract void selectItem(String code);

    public abstract void dispense();

    public abstract void refund();


}

class IdleState extends VendingMachineState {

    IdleState(VendingMachine vendingMachine) {
        super(vendingMachine);
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Please Insert Coin first");
    }

    @Override
    public void selectItem(String code) {
        System.out.println("Selecting item");
        // check for the inventory first
        if (vendingMachine.getInventory().isAvailable(code)) {
            vendingMachine.setSelectedItem(code);
            vendingMachine.setMachineState(new ItemSelectedState(vendingMachine));
        } else {
            System.out.println("Item out of stock");
        }
    }

    @Override
    public void dispense() {
        System.out.println("Pehlse item select kar badmein dispense karna");

    }

    @Override
    public void refund() {
        System.out.println("Kis chiz ka refund , item le pehle");
    }
}

class HasMoneyState extends VendingMachineState {

    public HasMoneyState(VendingMachine machine) {
        super(machine);
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Coding alredy inserted");
    }

    @Override
    public void selectItem(String code) {
        System.out.println("Item selected earlier");
    }

    @Override
    public void dispense() {
        System.out.println("Dispensing item");
        vendingMachine.dispense();
    }

    @Override
    public void refund() {
        System.out.println("Item toh lele pehel then refund manga abhi paise dal");
    }
}

class ItemSelectedState extends VendingMachineState {

    public ItemSelectedState(VendingMachine machine) {
        super(machine);
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Amount added to vending machine " + coin.getCoinValue() / 100);
        vendingMachine.addBalance(coin.getCoinValue());
        vendingMachine.setMachineState(new HasMoneyState(vendingMachine));
    }

    @Override
    public void selectItem(String code) {
        System.out.println("Item selected earlier");
    }

    @Override
    public void dispense() {
        System.out.println("Item selected money pending");
    }

    @Override
    public void refund() {
        System.out.println("Item selected money pending not refund");
    }
}

class DispensingState extends VendingMachineState {

    public DispensingState(VendingMachine machine) {
        super(machine);
    }

    @Override
    public void insertCoin(Coin coin) {
        System.out.println("Currently dispensing. Please wait.");
    }

    @Override
    public void selectItem(String code) {
        System.out.println("Currently dispensing. Please wait.");
    }

    @Override
    public void dispense() {
        System.out.println("in Progress");
    }

    @Override
    public void refund() {
        System.out.println("Dispensing in progress. Refund not allowed.");
    }
}


class VendingMachine {

    private static VendingMachine instance;
    private Inventory inventory = new Inventory();
    private double balance = 0;
    private VendingMachineState machineState;
    private String selectedItem;

    public static synchronized VendingMachine getInstance() {
        if (instance == null) {
            instance = new VendingMachine();
        }

        return instance;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setSelectedItem(String item) {
        this.selectedItem = item;
    }

    public void setMachineState(VendingMachineState state) {
        this.machineState = state;
    }

    public void addBalance(double value) {
        this.balance = value;
    }

    public void startMachine() {
        this.machineState = new IdleState(this);
    }

    public void dispense() {

        Item inventoryItem = inventory.getItem(selectedItem);
        double price = inventoryItem.getPrice();

        if (balance >= price) {

            inventory.reduceStock(selectedItem);
            balance -= price;
            System.out.println("Item dispensed" + inventoryItem.getName());

            if (balance > 0) {
                System.out.println("Returning change " + balance);
            }

        } else {
            System.out.println("Price kam hain");
            reset();
            setMachineState(new IdleState(this));
        }
    }

    public void reset() {
        selectedItem = null;
        balance = 0;
    }

    public void selectItem(String code) {
        machineState.selectItem(code);
    }

    public void insertCoin(Coin coin) {
        machineState.insertCoin(coin);
    }

    public void dispenseItem() {
        machineState.dispense();
    }

    public void refundItem() {
        machineState.refund();
    }
}

class Main {
    public static void main(String[] args) {

        VendingMachine vendingMachine = VendingMachine.getInstance();

        Item item1 = new Item("AB1", "Pepsi", 20);
        Item item2 = new Item("AB2", "Sprite", 15);

        Inventory inventory = new Inventory();
        inventory.addItem("AB1", item1, 4);
        inventory.addItem("AB2", item2, 2);

        vendingMachine.setInventory(inventory);

        vendingMachine.startMachine();
        vendingMachine.selectItem("AB2");
        vendingMachine.insertCoin(Coin.TWENTY);
        vendingMachine.dispense();


    }
}