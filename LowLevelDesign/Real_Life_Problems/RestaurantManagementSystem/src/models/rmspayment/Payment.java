package models.rmspayment;

import constants.PaymentStatus;

import java.util.Date;

public abstract class Payment {
    private  int paymentId ;
    private  float amount;
    private PaymentStatus status;
    private Date timestamp;

    public abstract void startTransaction();
}
