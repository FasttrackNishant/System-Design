package shubh.models;

import shubh.enums.GateType;

import java.time.LocalDateTime;

public class EntryGate extends Gate {

    public EntryGate(String id)
    {
        super(id);
    }

    @Override
    public GateType getType(){
        return GateType.ENTRY;
    }

    public  Ticket parkVehicle(Vehicle vehicle , LocalDateTime entryTime)
    {
        return ParkingLost.getInstance().parkVehicle(vehicle,entryTime);
    }

}
