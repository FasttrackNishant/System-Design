package behavioural.statedesign.solution;

public class ItemSelectedState implements MachineState {
        @Override
        public void selectItem(VendingMachine context, String itemCode) {
            System.out.println("Item already selected: " + context.getSelectedItem());
        }

        @Override
        public void insertCoin(VendingMachine context, double amount) {
            System.out.println("Inserted $" + amount + " for item: " + context.getSelectedItem());
            context.setInsertedAmount(amount);
            context.setMachinestate(new HasMoneyState());
        }

        @Override
        public void dispense(VendingMachine context) {
            System.out.println("Insert coin before dispensing.");
        }
    }