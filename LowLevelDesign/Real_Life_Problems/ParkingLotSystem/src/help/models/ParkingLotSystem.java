package help.models;

import help.interfaces.ParkingSpot;
import help.interfaces.Vehicle;
import help.models.gate.Entrance;
import help.models.gate.Exit;
import help.models.pricing.ParkingRate;
import help.models.ticket.ParkingTicket;

import java.util.HashMap;
import java.util.Map;

public class ParkingLotSystem {

    private int id;
    private String name;
    private  String address;
    private final  int MAX_CAPACITY = 400000;
    private HashMap<String, Entrance> entrances;
    private HashMap<String, Exit> exits;
    private ParkingRate parkingRate;
    private  Map<String,DisplayBoard> displayBoards;
    private  Map<String, ParkingTicket> parkingTickets;
    private static ParkingLotSystem parkingLotInstance ;
    private Map<String, ParkingSpot> parkingSpots;

    // singleton design pattern
    private ParkingLotSystem() {}

    public static ParkingLotSystem getInstance()
    {
        if(parkingLotInstance == null)
        {
            parkingLotInstance = new ParkingLotSystem();
        }
        return parkingLotInstance;
    }

    public ParkingTicket getParkingTicket(Vehicle vehicle)
    {
        return null;
    }

    public boolean isFull()
    {
        return false;
    }

    public boolean addEntrace(Entrance entrance)
    {
        return false;
    }

    public boolean addExit(Exit exit)
    {
        return false;
    }

}
