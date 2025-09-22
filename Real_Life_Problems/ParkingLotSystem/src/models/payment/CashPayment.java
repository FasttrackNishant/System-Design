package models.payment;

import interfaces.Payment;

public class CashPayment extends Payment {
    @Override
    public boolean InitiateTransation() {
        return false;
    }
}
