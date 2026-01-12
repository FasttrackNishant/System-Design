package behavioural.statedesign.solution;

public class IdleState implements MachineState{

   @Override
    public void selectItem(VendingMachine context, String itemCode) {
       System.out.println("Please select Item");;
       context.setSelectedItem(itemCode);;
       context.setMachinestate(new ItemSelectedState());
    }

    @Override
    public void insertCoin(VendingMachine context, double coinAmount) {
        System.out.println("Machine is in IDLE State");
    }

    @Override
    public void dispense(VendingMachine context) {
        System.out.println("Dispense not possible");
    }
}
