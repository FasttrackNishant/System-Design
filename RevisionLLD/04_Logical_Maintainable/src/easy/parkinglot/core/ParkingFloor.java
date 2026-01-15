package easy.parkinglot.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingFloor{

    private int floorNumber;
    private Map<String,ParkingSpot> spots;

    public ParkingFloor(int floorNumber){
        this.floorNumber = floorNumber;
        this.spots = new ConcurrentHashMap<>();
    }

    public void addSpot(ParkingSpot spot){
        if(spot == null){
            System.out.println("Not Valid Spot");
            return;
        }

        spots.put(spot.getSpotId(),spot);
    }

    // Find avaible spot
    public ParkingSpot findAvailableSpot(Vehicle vehicle){

        for(ParkingSpot spot : spots.values()){

            if(!spot.isOccupied() && spot.canFitVehicle(vehicle) )
            {
                return spot;
            }
        }

        return null;
    }
}
