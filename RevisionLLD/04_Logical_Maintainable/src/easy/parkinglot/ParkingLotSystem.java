package easy.parkinglot;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingLotSystem{

    private static ParkingLotSystem instance ;
    private final List<ParkingFloor> floors = new ArrayList<>();
    private  Map<String,ParkingTicket> activeTickets = new ConcurrentHashMap<>();
    private FeeStrategy feeStrategy ;


    private ParkingLotSystem() {
        this.feeStrategy = new FlatRateFeeStrategy();
        this.activeTickets = new ConcurrentHashMap<>();
    }

    public static synchronized ParkingLotSystem getInstance(){

        if(instance == null){
            instance = new ParkingLotSystem();
        }

        return instance;
    }

    public void addFloor(ParkingFloor floor){
        floors.add(floor);
    }

    public void setFeeStrategy(FeeStrategy feeStrategy){
        this.feeStrategy = feeStrategy;
    }


    public ParkingTicket parkVehicle(Vehicle vehicle){
        for(ParkingFloor floor : floors){
            ParkingSpot availableSpot = floor.findAvailableSpot(vehicle);

            if(availableSpot != null){
                availableSpot.parkVehicle(vehicle);
                ParkingTicket ticket = new ParkingTicket(vehicle,availableSpot);
                activeTickets.put(vehicle.getLiseceNumber(),ticket);
                return ticket;
            }
        }
        return  null;
    }

    public double unparkVehicle(String lisenceNumber){
        ParkingTicket currentTicket =  activeTickets.get(lisenceNumber);
        ParkingSpot currentSpot = currentTicket.getParkingSpot();
        currentSpot.unparkVehicle();
        currentTicket.setExitTimeStamp();
        System.out.println(currentTicket);
       return feeStrategy.calculateFee(currentTicket);
    }
}