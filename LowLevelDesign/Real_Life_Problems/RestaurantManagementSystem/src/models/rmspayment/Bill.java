package models.rmspayment;

public class Bill {

    private  int billId ;
    private float amount ;
    private float tax;
    private  float tip;
    private boolean isPaid ;

    public boolean generateBill(){
        return true;
    }

    public  boolean updateBill(){
        return true;
    }


}
