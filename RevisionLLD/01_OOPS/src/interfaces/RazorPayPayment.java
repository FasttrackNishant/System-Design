package interfaces;

public class RazorPayPayment implements PaymentGateway {

    @Override
    public void initiatePayment(double  amount){
        System.out.println("Razorpay Payment Initiated"+ amount);
    }

}