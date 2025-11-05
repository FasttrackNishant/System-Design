package help.models.payment;

import help.interfaces.Payment;

public class CashPayment extends Payment {
    @Override
    public boolean InitiateTransation() {
        return false;
    }
}
