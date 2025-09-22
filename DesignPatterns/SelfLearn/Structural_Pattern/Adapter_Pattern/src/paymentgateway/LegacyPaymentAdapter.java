package paymentgateway;

public class LegacyPaymentAdapter implements PaymentProcessor{

    private final LegacyPaymentGateWay legacyGateway;
    private long currentRef;

    public LegacyPaymentAdapter(LegacyPaymentGateWay gateWay){
        this.legacyGateway = gateWay;
    }

    @Override
    public boolean getPaymentStatus() {
        return legacyGateway.checkStatus(currentRef);
    }

    @Override
    public void processPayment(double amount, String currency) {

        System.out.println("Adapter calling process payment");
        legacyGateway.executeTransaction(amount,currency);
        currentRef = legacyGateway.getTransactionRef();

    }

    @Override
    public String getTransactionId() {
        return "LegacyTxn" + currentRef;
    }
}
