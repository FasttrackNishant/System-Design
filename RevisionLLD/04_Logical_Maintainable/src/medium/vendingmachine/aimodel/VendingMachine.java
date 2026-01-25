package medium.vendingmachine.aimodel;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/*
 * ==============================
 * COIN (configuration friendly)
 * ==============================
 */
class Coin {
    private final int valueInPaise;

    public Coin(int valueInPaise) {
        this.valueInPaise = valueInPaise;
    }

    public int getValue() {
        return valueInPaise;
    }

    @Override
    public String toString() {
        return "₹" + valueInPaise / 100;
    }
}

/*
 * ==============================
 * ITEM
 * ==============================
 */
class Item {
    private final String code;
    private final String name;
    private final int priceInPaise;

    public Item(String code, String name, int priceInPaise) {
        this.code = code;
        this.name = name;
        this.priceInPaise = priceInPaise;
    }

    public int getPrice() {
        return priceInPaise;
    }

    public String getName() {
        return name;
    }
}

/*
 * ==============================
 * INVENTORY
 * ==============================
 */
class Inventory {
    private final Map<String, Item> items = new HashMap<>();
    private final Map<String, Integer> stock = new HashMap<>();

    public void addItem(String code, Item item, int qty) {
        items.put(code, item);
        stock.put(code, qty);
    }

    public boolean isAvailable(String code) {
        return stock.getOrDefault(code, 0) > 0;
    }

    public Item getItem(String code) {
        return items.get(code);
    }

    public void reduceStock(String code) {
        stock.put(code, stock.get(code) - 1);
    }
}

/*
 * ==============================
 * STATES
 * ==============================
 */
abstract class State {
    protected VendingMachine vm;

    State(VendingMachine vm) {
        this.vm = vm;
    }

    abstract void insertCoin(Coin coin);
    abstract void selectItem(String code);
    abstract void dispense();
    abstract void refund();
}

class IdleState extends State {
    IdleState(VendingMachine vm) { super(vm); }

    public void insertCoin(Coin coin) {
        vm.addCoin(coin);
        vm.setState(new HasMoneyState(vm));
    }

    public void selectItem(String code) {
        if (!vm.getInventory().isAvailable(code)) {
            System.out.println("Item out of stock");
            return;
        }
        vm.setSelectedItem(code);
        vm.setState(new ItemSelectedState(vm));
    }

    public void dispense() {
        System.out.println("Select item first");
    }

    public void refund() {
        System.out.println("Nothing to refund");
    }
}

class ItemSelectedState extends State {
    ItemSelectedState(VendingMachine vm) { super(vm); }

    public void insertCoin(Coin coin) {
        vm.addCoin(coin);
        vm.setState(new HasMoneyState(vm));
    }

    public void selectItem(String code) {
        System.out.println("Item already selected");
    }

    public void dispense() {
        System.out.println("Insert money first");
    }

    public void refund() {
        vm.refundInternal();
    }
}

class HasMoneyState extends State {
    HasMoneyState(VendingMachine vm) { super(vm); }

    public void insertCoin(Coin coin) {
        vm.addCoin(coin);
    }

    public void selectItem(String code) {
        System.out.println("Item already selected");
    }

    public void dispense() {
        vm.processDispense();
    }

    public void refund() {
        vm.refundInternal();
    }
}

class DispensingState extends State {
    DispensingState(VendingMachine vm) { super(vm); }

    public void insertCoin(Coin coin) {
        System.out.println("Dispensing in progress");
    }

    public void selectItem(String code) {
        System.out.println("Dispensing in progress");
    }

    public void dispense() {
        System.out.println("Already dispensing");
    }

    public void refund() {
        System.out.println("Refund not allowed during dispense");
    }
}

/*
 * ==============================
 * VENDING MACHINE
 * ==============================
 */
class VendingMachine {
    private final Inventory inventory = new Inventory();
    private final Map<Coin, Integer> coinInventory = new HashMap<>();
    private final Map<Coin, Integer> insertedCoins = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private State state;
    private String selectedItem;
    private int balanceInPaise = 0;

    public VendingMachine() {
        state = new IdleState(this);
    }

    /* ---------- State helpers ---------- */

    public void setState(State state) {
        this.state = state;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setSelectedItem(String code) {
        this.selectedItem = code;
    }

    /* ---------- Coin handling ---------- */

    public void addCoin(Coin coin) {
        balanceInPaise += coin.getValue();
        insertedCoins.put(coin, insertedCoins.getOrDefault(coin, 0) + 1);
        coinInventory.put(coin, coinInventory.getOrDefault(coin, 0) + 1);
        System.out.println("Inserted " + coin);
    }

    /* ---------- Dispense ---------- */

    public void processDispense() {
        lock.lock();
        try {
            setState(new DispensingState(this));

            Item item = inventory.getItem(selectedItem);
            if (balanceInPaise < item.getPrice()) {
                System.out.println("Insufficient balance");
                refundInternal();
                return;
            }

            int change = balanceInPaise - item.getPrice();
            if (!canReturnChange(change)) {
                System.out.println("Cannot return exact change. Refund initiated.");
                refundInternal();
                return;
            }

            inventory.reduceStock(selectedItem);
            returnChange(change);

            System.out.println("Dispensed: " + item.getName());
            reset();
        } finally {
            setState(new IdleState(this));
            lock.unlock();
        }
    }

    /* ---------- Refund ---------- */

    public void refundInternal() {
        lock.lock();
        try {
            for (Map.Entry<Coin, Integer> e : insertedCoins.entrySet()) {
                System.out.println("Refunding " + e.getValue() + " x " + e.getKey());
                coinInventory.put(e.getKey(),
                        coinInventory.get(e.getKey()) - e.getValue());
            }
            reset();
        } finally {
            setState(new IdleState(this));
            lock.unlock();
        }
    }

    /* ---------- Change logic ---------- */

    private boolean canReturnChange(int change) {
        int remaining = change;
        List<Coin> coins = new ArrayList<>(coinInventory.keySet());
        coins.sort((a, b) -> b.getValue() - a.getValue());

        for (Coin coin : coins) {
            int usable = Math.min(coinInventory.get(coin),
                    remaining / coin.getValue());
            remaining -= usable * coin.getValue();
        }
        return remaining == 0;
    }

    private void returnChange(int change) {
        int remaining = change;
        List<Coin> coins = new ArrayList<>(coinInventory.keySet());
        coins.sort((a, b) -> b.getValue() - a.getValue());

        for (Coin coin : coins) {
            int usable = Math.min(coinInventory.get(coin),
                    remaining / coin.getValue());
            if (usable > 0) {
                System.out.println("Returning " + usable + " x " + coin);
                coinInventory.put(coin, coinInventory.get(coin) - usable);
                remaining -= usable * coin.getValue();
            }
        }
    }

    private void reset() {
        balanceInPaise = 0;
        insertedCoins.clear();
        selectedItem = null;
    }

    /* ---------- API ---------- */

    public void insertCoin(Coin coin) {
        state.insertCoin(coin);
    }

    public void selectItem(String code) {
        state.selectItem(code);
    }

    public void dispense() {
        state.dispense();
    }

    public void refund() {
        state.refund();
    }
}

/*
 * ==============================
 * MAIN
 * ==============================
 */
class Main {
    public static void main(String[] args) {

        VendingMachine vm = new VendingMachine();

        Coin five = new Coin(500);
        Coin ten = new Coin(1000);
        Coin twenty = new Coin(2000);

        vm.getInventory().addItem("A1", new Item("A1", "Pepsi", 1500), 5);
        vm.getInventory().addItem("A2", new Item("A2", "Sprite", 1200), 5);

        vm.selectItem("A2");
        vm.insertCoin(ten);
        vm.insertCoin(five);
        vm.dispense();
    }
}
