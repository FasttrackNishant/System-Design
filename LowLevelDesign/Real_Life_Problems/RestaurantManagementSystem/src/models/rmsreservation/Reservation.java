package models.rmsreservation;

import constants.ReservationStatus;
import models.Customer;
import models.observer.Observer;
import models.observer.Subject;
import models.rmsorder.Table;

import java.util.ArrayList;
import  java.util.Date;
import java.util.List;

public class Reservation implements Subject {

     private List<Observer> observerList = new ArrayList<>();

     int reservationId ;
     private  Date reservationTimeStamp ;
     private  int totalCount ;
     private ReservationStatus status;
     private  String instructions;
     private Customer customer;
     private Table table;

     public  boolean updateReservation(int count)
     {
         return true;
     }

     public ReservationStatus getStatus(){
          return this.status;
     }

     public void setStatus(ReservationStatus status){
          this.status = status;

          // update all obsevers
          notifyObservers();
     }

     @Override
     public void addObserver(Observer obs) {
          if(!observerList.contains(obs))
          {
               observerList.add(obs);
          }
     }

     @Override
     public void removeObserver(Observer obs) {
          observerList.remove(obs);
     }

     @Override
     public void notifyObservers() {
          for(Observer obs : observerList){
               obs.update();
          }
     }
}
