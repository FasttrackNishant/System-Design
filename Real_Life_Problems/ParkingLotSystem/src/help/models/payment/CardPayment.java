package help.models.payment;

import help.interfaces.Payment;

public class CardPayment extends Payment {

    @Override
    public boolean InitiateTransation() {
        return false;
    }
}
