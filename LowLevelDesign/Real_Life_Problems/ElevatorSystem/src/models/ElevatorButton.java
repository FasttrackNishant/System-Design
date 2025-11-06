package models;

public class ElevatorButton extends  Button{
    private  int destinationFloorNumber ;

    @Override
    public void press() {

    }

    @Override
    public boolean isPressed() {
        return false;
    }

    @Override
    public boolean isStatus() {
        return super.isStatus();
    }

    // override
}
