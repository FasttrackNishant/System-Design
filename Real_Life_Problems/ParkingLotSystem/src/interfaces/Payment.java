package interfaces;

import constants.PaymentStatus;
import  java.util.Date;

public abstract class Payment {

    private  String TransactionId;

    private  double amount;

    private PaymentStatus status;

    private Date timestamp;

    public  abstract boolean InitiateTransation();


}
