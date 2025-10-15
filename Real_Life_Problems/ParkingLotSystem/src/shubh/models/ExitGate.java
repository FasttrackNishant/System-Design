package shubh.models;

import help.models.ParkingLotSystem;
import shubh.enums.GateType;

import java.time.LocalDateTime;

public class ExitGate extends Gate{

    public  ExitGate(String id) {
        super(id);
    }

    @Override
    public GateType getType() {
        return GateType.EXIT;
    }
    
    public void unparkVehicle(String ticketId , LocalDateTime exitTime , PaymentMode paymentMode)
    {
        ParkingLotSystem.getInstance().unparkVehicle(ticketId,exitTime,paymentMode);
    }
}
