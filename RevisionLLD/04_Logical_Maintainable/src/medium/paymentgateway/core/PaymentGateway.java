package medium.paymentgateway.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

enum PaymentMethod {
    CREDIT_CARD,
    UPI,
    PAYPAL
}

enum PaymentStatus {
    SUCCESS,
    FAILURE,
    INITIATED
}

class PaymentRequest {

    private String transactionId;
    private String payerId;
    private double amount;
    private PaymentMethod paymentMethod;

    private PaymentRequest(PaymentRequestBuilder builder) {
        this.transactionId = UUID.randomUUID().toString();
        this.payerId = builder.payerId;
        this.amount = builder.amount;
        this.paymentMethod = builder.paymentMethod;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public double getAmount() {
        return amount;
    }

    public static class PaymentRequestBuilder {

        private String payerId;
        private double amount;
        private PaymentMethod paymentMethod;

        public PaymentRequestBuilder setPayerId(String payerId) {
            this.payerId = payerId;
            return this;
        }

        public PaymentRequestBuilder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public PaymentRequestBuilder setPaymentMethod(PaymentMethod method) {
            this.paymentMethod = method;
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(this);
        }
    }
}

class PaymentResponse {
    private PaymentStatus status;
    private String message;

    public PaymentResponse(PaymentStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "message='" + message + '\'' +
                ", status=" + status +
                '}';
    }
}

class Transaction {
    private String id;
    private PaymentStatus status;
    private PaymentRequest request;
    private LocalDateTime timestamp;

    public Transaction(PaymentRequest request) {
        this.id = UUID.randomUUID().toString();
        this.status = PaymentStatus.INITIATED;
        this.request = request;
        this.timestamp = LocalDateTime.now();
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentRequest getRequest() {
        return request;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", \n status= " + status +
                ", timestamp=" + timestamp +
                '}';
    }
}

interface PaymentProcessor {
    PaymentResponse processPayment(PaymentRequest request);
}

abstract class AbstractPaymentProcessor implements PaymentProcessor {

    private static final int MAX_RETRIES = 3;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        int attempts = 0;
        PaymentResponse response;
        do {
            response = executeProcess(request);
            attempts++;
        } while (response.getStatus() == PaymentStatus.FAILURE && attempts < MAX_RETRIES);

        return response;
    }

    protected abstract PaymentResponse executeProcess(PaymentRequest request);
}

class CreditCardPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    protected PaymentResponse executeProcess(PaymentRequest request) {
        System.out.println("Credit card payment ");
        return new PaymentResponse(PaymentStatus.SUCCESS, "Ho gaya credit card se");
    }
}

class UpiPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    protected PaymentResponse executeProcess(PaymentRequest request) {
        System.out.println("UPI  payment ");
        return new PaymentResponse(PaymentStatus.SUCCESS, "Ho gaya UPI  se");
    }
}

class PaypalPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    protected PaymentResponse executeProcess(PaymentRequest request) {
        System.out.println("Paypal payment ");
        return new PaymentResponse(PaymentStatus.FAILURE, "Ho gaya Paypal se");
    }
}


class PaymentProcessorFactory {

    public static PaymentProcessor getProcessor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD -> new CreditCardPaymentProcessor();
            case UPI -> new UpiPaymentProcessor();
            case PAYPAL -> new PaypalPaymentProcessor();
            default -> throw new IllegalArgumentException("Diff type of processor asked");
        };
    }
}

interface PaymentObserver {
    void onTransactionUpdate(Transaction transaction);
}

class CustomerNotifier implements PaymentObserver {

    @Override
    public void onTransactionUpdate(Transaction transaction) {
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            System.out.println("--- CUSTOMER EMAIL ---");
            System.out.println("Your payment of " + transaction.getRequest().getAmount() + " was successful. Transaction ID: " + transaction.getId());
            System.out.println("----------------------");
        }
    }

}

class MerchantNotifier implements PaymentObserver {

    @Override
    public void onTransactionUpdate(Transaction transaction) {
        System.out.println("--- MERCHANT NOTIFICATION ---");
        System.out.println("Transaction " + transaction.getId() + " status updated to: " + transaction.getStatus());
        System.out.println("-----------------------------");
    }

}


class PaymentGatewayService {

    private static PaymentGatewayService instance;
    private List<PaymentObserver> observers = new ArrayList<>();

    private PaymentGatewayService() {
    }

    public static synchronized PaymentGatewayService getInstance() {
        if (instance == null) {
            instance = new PaymentGatewayService();
        }
        return instance;
    }

    public Transaction processPayment(PaymentRequest request) {
        Transaction transaction = new Transaction(request);

        try {

            PaymentProcessor processor = PaymentProcessorFactory.getProcessor(request.getPaymentMethod());

            PaymentResponse response = processor.processPayment(request);
            transaction.setStatus(response.getStatus());
            notifyAll(transaction);

        } catch (Exception e) {
            System.err.println("Payment processing failed: " + e.getMessage());
            transaction.setStatus(PaymentStatus.FAILURE);
            System.out.println(e.getMessage());
        }

        return transaction;
    }

    public void addObserver(PaymentObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(PaymentObserver observer) {
        observers.remove(observer);
    }

    private void notifyAll(Transaction transaction) {

        for (PaymentObserver observer : observers) {
            observer.onTransactionUpdate(transaction);
        }
    }
}

class Main {

    public static void main(String[] args) {

        PaymentGatewayService gatewayService = PaymentGatewayService.getInstance();

        PaymentObserver customerNotifier = new CustomerNotifier();
        PaymentObserver merchantNotifier = new MerchantNotifier();

        PaymentRequest request = new PaymentRequest.PaymentRequestBuilder().setAmount(344.2)
                .setPayerId("Dev").setAmount(342.2).setPaymentMethod(PaymentMethod.CREDIT_CARD).build();

        gatewayService.addObserver(customerNotifier);
        gatewayService.addObserver(merchantNotifier);

        Transaction transaction = gatewayService.processPayment(request);
    }
}