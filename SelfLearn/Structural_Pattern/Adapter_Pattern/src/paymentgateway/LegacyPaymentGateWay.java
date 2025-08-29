package paymentgateway;

public class LegacyPaymentGateWay {

    private long transactionRef ;
    private  boolean isPaymentDone;

    public void executeTransaction(double amount,String ref)
    {
        System.out.println("Executing legacy payment");
        isPaymentDone = true;
        transactionRef = System.nanoTime();
        System.out.println("Legacy Payment Done");
    }

    public  boolean checkStatus(long transactionRef)
    {
        System.out.println("Checking Status for"+transactionRef);
        return  isPaymentDone;
    }

    public long getTransactionRef(){
        return transactionRef;
    }


}
