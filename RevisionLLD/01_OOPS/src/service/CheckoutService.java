package service;
import interfaces.PaymentGateway;

public class CheckoutService{

    private PaymentGateway paymentGateway;

    public CheckoutService(PaymentGateway paymentGateway){
        this.paymentGateway = paymentGateway;
    }

    public void setPaymentGateway(PaymentGateway paymentGateway)
    {
        this.paymentGateway = paymentGateway;
    }

    public void checkout(double  amount){
        paymentGateway.initiatePayment(amount);
    }
}