package models.rmsreservation;

import constants.ReservationStatus;
import models.observer.Observer;

import java.util.Date;

public class Notification implements Observer {

    private int notificationId;
    private Date creationTime;
    private String content;
    private Reservation reservation;

    Notification(Reservation reservation){
        this.reservation = reservation;
        this.reservation.addObserver(this);
    }

    public void sendNotification(){

    }

    @Override
    public void update(){
        if(reservation.getStatus() == ReservationStatus.CONFIRMED){
            System.out.println("Reservation ho gayi ");
        }
    }
}
