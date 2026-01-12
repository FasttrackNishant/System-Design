package behavioural.statedesign.violation;

public class VendingMachine {

        private enum State {
            IDLE, ITEM_SELECTED, HAS_MONEY, DISPENSING
        }

        private State currentState = State.IDLE;
        private String selectedItem = "";
        private double insertedAmount = 0.0;

        public void selectItem(String itemCode) {
            switch (currentState) {
                case IDLE:
                    selectedItem = itemCode;
                    currentState = State.ITEM_SELECTED;
                    break;
                case ITEM_SELECTED:
                    System.out.println("Item already selected");
                    break;
                case HAS_MONEY:
                    System.out.println("Payment already received for item");
                    break;
                case DISPENSING:
                    System.out.println("Currently dispensing");
                    break;
            }
        }

        public void insertCoin(double amount) {
            switch (currentState) {
                case IDLE:
                    System.out.println("No item selected");
                    break;
                case ITEM_SELECTED:
                    insertedAmount = amount;
                    System.out.println("Inserted $" + amount + " for item");
                    currentState = State.HAS_MONEY;
                    break;
                case HAS_MONEY:
                    System.out.println("Money already inserted");
                    break;
                case DISPENSING:
                    System.out.println("Currently dispensing");
                    break;
            }
        }

        public void dispenseItem() {
            switch (currentState) {
                case IDLE:
                    System.out.println("No item selected");
                    break;
                case ITEM_SELECTED:
                    System.out.println("Please insert coin first");
                    break;
                case HAS_MONEY:
                    System.out.println("Dispensing item '" + selectedItem + "'");
                    currentState = State.DISPENSING;

                    // Simulate delay and completion
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println("Item dispensed successfully.");
                    resetMachine();
                    break;
                case DISPENSING:
                    System.out.println("Already dispensing. Please wait.");
                    break;
            }
        }

        private void resetMachine() {
            selectedItem = "";
            insertedAmount = 0.0;
            currentState = State.IDLE;
        }
    }