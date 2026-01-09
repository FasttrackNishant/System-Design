package statedesign.solution;

public interface MachineState {
    // select item , dispense , insert money

    void selectItem(VendingMachine context , String  itemCode);
    void insertCoin(VendingMachine context , double coinAmount);
    void dispense(VendingMachine context);





}
