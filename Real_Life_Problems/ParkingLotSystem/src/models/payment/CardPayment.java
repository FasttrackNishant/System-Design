package models.payment;

import interfaces.Payment;

public class CardPayment extends Payment {

    @Override
    public boolean InitiateTransation() {
        return false;
    }
}
