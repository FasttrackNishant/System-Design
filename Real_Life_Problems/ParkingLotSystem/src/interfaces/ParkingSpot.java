package interfaces;

public abstract class ParkingSpot {

    private int id;

    private boolean isPresent;

    // this is composition
    private Vehicle vehicle;

    public abstract boolean getIsFree();

}
