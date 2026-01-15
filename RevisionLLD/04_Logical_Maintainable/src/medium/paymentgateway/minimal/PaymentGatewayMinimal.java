package medium.paymentgateway.minimal;

enum PaymentMethod {
    CREDIT_CARD,
    UPI,
    PAYPAL
}

enum PaymentStatus {
    SUCCESS,
    FAILURE
}

class PaymentRequest {
    private final double amount;
    private final PaymentMethod method;

    public PaymentRequest(double amount, PaymentMethod method) {
        this.amount = amount;
        this.method = method;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }
}

class PaymentResponse {
    private final PaymentStatus status;
    private final String message;

    public PaymentResponse(PaymentStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status + " : " + message;
    }
}

/* ================== CORE ABSTRACTION ================== */

interface PaymentProcessor {
    PaymentResponse process(PaymentRequest request);
}

/* ================== IMPLEMENTATIONS ================== */

class CreditCardProcessor implements PaymentProcessor {
    @Override
    public PaymentResponse process(PaymentRequest request) {
        return new PaymentResponse(PaymentStatus.SUCCESS, "Paid via Credit Card");
    }
}

class UpiProcessor implements PaymentProcessor {
    @Override
    public PaymentResponse process(PaymentRequest request) {
        return new PaymentResponse(PaymentStatus.SUCCESS, "Paid via UPI");
    }
}

class PaypalProcessor implements PaymentProcessor {
    @Override
    public PaymentResponse process(PaymentRequest request) {
        return new PaymentResponse(PaymentStatus.FAILURE, "Paypal payment failed");
    }
}

/* ================== FACTORY (JUSTIFIED) ================== */

class PaymentProcessorFactory {

    public static PaymentProcessor getProcessor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD -> new CreditCardProcessor();
            case UPI -> new UpiProcessor();
            case PAYPAL -> new PaypalProcessor();
        };
    }
}

/* ================== SERVICE ================== */

class PaymentGateway {

    public PaymentResponse processPayment(PaymentRequest request) {
        PaymentProcessor processor =
                PaymentProcessorFactory.getProcessor(request.getMethod());
        return processor.process(request);
    }
}

/* ================== DEMO ================== */

class Main {
    public static void main(String[] args) {

        PaymentGateway gateway = new PaymentGateway();

        PaymentRequest request =
                new PaymentRequest(500.0, PaymentMethod.CREDIT_CARD);

        PaymentResponse response = gateway.processPayment(request);

        System.out.println(response);
    }
}
