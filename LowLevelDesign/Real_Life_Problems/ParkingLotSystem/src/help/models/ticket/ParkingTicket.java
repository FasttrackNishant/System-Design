package help.models.ticket;
import help.constants.TicketStatus;
import help.interfaces.Vehicle;
import help.models.gate.Entrance;
import  help.models.gate.Exit;
import help.interfaces.Payment;

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
