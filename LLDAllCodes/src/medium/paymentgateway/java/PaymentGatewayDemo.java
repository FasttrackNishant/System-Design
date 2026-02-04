package easy.snakeandladder.java;

enum PaymentMethod {
    CREDIT_CARD,
    PAYPAL,
    UPI
}


enum PaymentStatus {
    INITIATED,
    SUCCESSFUL,
    FAILED
}



class PaymentProcessorFactory {
    public static PaymentProcessor getProcessor(PaymentMethod method) {
        switch (method) {
            case CREDIT_CARD:
                return new CreditCardProcessor();
            case UPI:
                return new UPIProcessor();
            case PAYPAL:
                return new PayPalProcessor();
            // case BANK_TRANSFER:
            //     return new BankTransferProcessor();
            default:
                throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
    }
}


class PaymentRequest {
    private final String transactionId;
    private final String payerId;
    private final double amount;
    private final String currency;
    private final PaymentMethod paymentMethod;
    private final Map<String, String> paymentDetails;

    public PaymentRequest(Builder builder) {
        this.transactionId = UUID.randomUUID().toString();
        this.payerId = builder.payerId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.paymentMethod = builder.paymentMethod;
        this.paymentDetails = builder.paymentDetails;
    }

    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public  String getCurrency() { return currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }

    public static class Builder {
        private String payerId;
        private double amount;
        private String currency;
        private PaymentMethod paymentMethod;
        private  Map<String, String> paymentDetails;

        public Builder payerId(String payerId) {
            this.payerId = payerId;
            return  this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public  Builder currency(String currency) {
            this.currency = currency;
            return  this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder paymentDetails(Map<String, String> paymentDetails) {
            this.paymentDetails = paymentDetails;
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(this);
        }
    }
}




class PaymentResponse {
    private final PaymentStatus status;
    private final String message;

    public PaymentResponse(PaymentStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public PaymentStatus getStatus() { return status; }
    public String getMessage() { return message; }
}





class Transaction {
    private final String id;
    private final PaymentRequest request;
    private PaymentStatus status;
    private final LocalDateTime timestamp;

    public Transaction(PaymentRequest request) {
        this.id = request.getTransactionId();
        this.request = request;
        this.status = PaymentStatus.INITIATED;
        this.timestamp = LocalDateTime.now();
    }

    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getId() { return id; }
    public PaymentStatus getStatus() { return status; }
    public PaymentRequest getRequest() { return request; }
}




class CustomerNotifier implements PaymentObserver {
    @Override
    public void onTransactionUpdate(Transaction transaction) {
        if (transaction.getStatus() == PaymentStatus.SUCCESSFUL) {
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




interface PaymentObserver {
    void onTransactionUpdate(Transaction transaction);
}








abstract class AbstractPaymentProcessor implements PaymentProcessor {
    private static final int MAX_RETRIES = 3;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        int attempts = 0;
        PaymentResponse response;
        do {
            response = doProcess(request);
            attempts++;
        } while (response.getStatus() == PaymentStatus.FAILED && attempts < MAX_RETRIES);
        return response;
    }

    protected abstract PaymentResponse doProcess(PaymentRequest request);
}





class CreditCardProcessor extends AbstractPaymentProcessor {
    @Override
    protected PaymentResponse doProcess(PaymentRequest request) {
        System.out.println("Processing credit card payment of amount " + request.getAmount() + " " + request.getCurrency());
        // Simulate interaction with Visa/Mastercard network
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "Credit Card payment successful.");
    }
}



interface PaymentProcessor {
    PaymentResponse processPayment(PaymentRequest request);
}



class PayPalProcessor extends AbstractPaymentProcessor {
    @Override
    protected PaymentResponse doProcess(PaymentRequest request) {
        System.out.println("Redirecting to PayPal for transaction " + request.getTransactionId());
        // Simulate PayPal API interaction
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "Paypal payment successful.");
    }
}


class UPIProcessor extends AbstractPaymentProcessor {
    @Override
    protected PaymentResponse doProcess(PaymentRequest request) {
        System.out.println("Processing UPI payment of " + request.getAmount() + " " + request.getCurrency());
        return new PaymentResponse(PaymentStatus.SUCCESSFUL, "UPI payment successful.");
    }
}





import java.util.*;
import java.time.LocalDateTime;

public class PaymentGatewayDemo {
    public static void main(String[] args) {
        // 1. Setup the gateway facade
        PaymentGatewayService paymentGateway = PaymentGatewayService.getInstance();

        // 2. Register observers to be notified of transaction events
        paymentGateway.addObserver(new MerchantNotifier());
        paymentGateway.addObserver(new CustomerNotifier());

        System.out.println("----------- SCENARIO 1: Successful Credit Card Payment -----------");
        // a. Merchant's backend creates a payment request
        PaymentRequest ccRequest = new PaymentRequest.Builder()
                .payerId("U-123")
                .amount(150.75)
                .currency("INR")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentDetails(Map.of("cardNumber", "1234..."))
                .build();

        // b. Merchant's backend sends it to the facade
        paymentGateway.processPayment(ccRequest);

        System.out.println("\n----------- SCENARIO 2: Successful PayPal Payment -----------");
        PaymentRequest paypalRequest = new PaymentRequest.Builder()
                .payerId("U-456")
                .amount(88.50)
                .currency("USD")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentDetails(Map.of("email", "customer@example.com"))
                .build();

        paymentGateway.processPayment(paypalRequest);
    }
}






class PaymentGatewayService {
    private static PaymentGatewayService instance;
    private final List<PaymentObserver> observers = new ArrayList<>();

    private  PaymentGatewayService() {}

    public static synchronized PaymentGatewayService getInstance() {
        if (instance == null) {
            instance = new PaymentGatewayService();
        }
        return  instance;
    }

    public void addObserver(PaymentObserver observer) { observers.add(observer); }
    public void removeObserver(PaymentObserver observer) { observers.remove(observer); }
    private void notifyObservers(Transaction transaction) {
        observers.forEach(o -> o.onTransactionUpdate(transaction));
    }

    public Transaction processPayment(PaymentRequest request) {
        Transaction transaction = new Transaction(request);
        try {
            PaymentProcessor processor = PaymentProcessorFactory.getProcessor(request.getPaymentMethod());
            PaymentResponse response = processor.processPayment(request);
            transaction.setStatus(response.getStatus());
        } catch (Exception e) {
            System.err.println("Payment processing failed: " + e.getMessage());
            transaction.setStatus(PaymentStatus.FAILED);
        }
        notifyObservers(transaction);
        return transaction;
    }
}
























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































