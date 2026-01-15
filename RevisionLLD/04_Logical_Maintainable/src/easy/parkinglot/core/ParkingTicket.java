package easy.parkinglot.core;

import java.util.Date;
import java.util.UUID;

public class ParkingTicket{

    private String ticketId;
    private Vehicle vehicle;
    private long entryTime;
    private long exitTime;
    private ParkingSpot spot;

    public ParkingTicket(Vehicle vehicle ,ParkingSpot spot){

        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.entryTime = new Date().getTime();
        this.spot = spot;
    }

    public long getEntryTime() {
        return entryTime;
    }

    public long getExitTime() {
        return exitTime;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getTicketId(){
        return this.ticketId;
    }

    public ParkingSpot getParkingSpot(){
        return this.spot;
    }

    public void setExitTimeStamp(){
        this.exitTime = new Date().getTime();
    }

    @Override
    public String toString() {
        return "ParkingTicket{" +
                "entryTime=" + entryTime +
                ", ticketId='" + ticketId + '\'' +
                ", vehicle=" + vehicle +
                ", exitTime=" + exitTime +
                ", spot=" + spot +
                '}';
    }
}