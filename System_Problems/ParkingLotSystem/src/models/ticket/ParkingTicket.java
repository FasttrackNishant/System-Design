package models.ticket;
import constants.TicketStatus;
import interfaces.Vehicle;
import models.gate.Entrance;
import  models.gate.Exit;
import interfaces.Payment;

import java.util.Date;

public class ParkingTicket {
    private int ticketNo;

    private  Date entryTimeStamp;

    private Date exitTimeStamp;

    private  double amount;

    private TicketStatus status;

    // aur
    private Vehicle vehicle;

    private Payment payment;

    private Entrance entrance;

    private  Exit exit;
}
