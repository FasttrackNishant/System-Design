package statedesign.solution;

public class Dispensing implements MachineState {
    @Override
    public void selectItem(VendingMachine context, String itemCode) {
        System.out.println("Please wait, dispensing in progress.");
    }

    @Override
    public void insertCoin(VendingMachine context, double amount) {
        System.out.println("Please wait, dispensing in progress.");
    }

    @Override
    public void dispense(VendingMachine context) {
        System.out.println("Already dispensing. Please wait.");
    }
}