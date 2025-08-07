package models.user;

import interfaces.Account;
import models.ticket.ParkingTicket;

public class Agent extends Account {

    public  boolean processTicket(ParkingTicket ticket)
    {
        // add impl
        return false;
    }


    @Override
    public boolean resetPassword() {
        return false;
    }
}
