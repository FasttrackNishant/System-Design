package statedesign.solution;

public class Main {
    public static void main(String[] args) {

        VendingMachine vendingMachine = new VendingMachine();

        vendingMachine.selectItem("PARLEG");
        vendingMachine.insertCoin(43);
        vendingMachine.dispense();

        vendingMachine.insertCoin(33);


    }
}
