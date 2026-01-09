package statedesign.solution;

public class VendingMachine {

    private String selectedItem = "" ;
    private double insertedAmount = 0.0;
    private MachineState currentMachineState;

    public VendingMachine() {
        this.currentMachineState = new IdleState();
    };

    public String getSelectedItem()
    {
        return this.selectedItem;
    }

    public double getInsertedAmount(){
        return this.insertedAmount;
    }

    public MachineState getMachineState(){
        return this.currentMachineState;
    }

    public void setSelectedItem(String itemCode){
        this.selectedItem = itemCode;
    }

    public void setInsertedAmount(double amount){
        this.insertedAmount = amount;
    }

    public void setMachinestate(MachineState state){
        this.currentMachineState = state;
    }

    public void selectItem(String itemcode){
        currentMachineState.selectItem(this,itemcode);
    }

    public  void insertCoin(double amount){
        currentMachineState.insertCoin(this,amount);
    }

    public void dispense(){
        currentMachineState.dispense(this);
    }

    public void reset(){
        this.currentMachineState = new IdleState();
        this.insertedAmount = 0.0;
        this.selectedItem = "";
    }

}
