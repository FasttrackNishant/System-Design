class PaymentRequest {
private:
    string transactionId;
    string payerId;
    double amount;
    string currency;
    PaymentMethod paymentMethod;
    map<string, string> paymentDetails;

public:
    class Builder {
    public:
        string payerId;
        double amount;
        string currency;
        PaymentMethod paymentMethod;
        map<string, string> paymentDetails;

        Builder& setPayerId(const string& payerId) {
            this->payerId = payerId;
            return *this;
        }

        Builder& setAmount(double amount) {
            this->amount = amount;
            return *this;
        }

        Builder& setCurrency(const string& currency) {
            this->currency = currency;
            return *this;
        }

        Builder& setPaymentMethod(PaymentMethod paymentMethod) {
            this->paymentMethod = paymentMethod;
            return *this;
        }

        Builder& setPaymentDetails(const map<string, string>& paymentDetails) {
            this->paymentDetails = paymentDetails;
            return *this;
        }

        PaymentRequest build() {
            return PaymentRequest(*this);
        }
    };

    PaymentRequest(const Builder& builder) 
        : transactionId(generateId()), payerId(builder.payerId), amount(builder.amount),
          currency(builder.currency), paymentMethod(builder.paymentMethod),
          paymentDetails(builder.paymentDetails) {}

    string getTransactionId() const { return transactionId; }
    double getAmount() const { return amount; }
    string getCurrency() const { return currency; }
    PaymentMethod getPaymentMethod() const { return paymentMethod; }
};









class PaymentResponse {
private:
    PaymentStatus status;
    string message;

public:
    PaymentResponse(PaymentStatus status, const string& message) 
        : status(status), message(message) {}

    PaymentStatus getStatus() const { return status; }
    string getMessage() const { return message; }
};












class Transaction {
private:
    string id;
    shared_ptr<PaymentRequest> request;
    PaymentStatus status;
    string timestamp;

public:
    Transaction(shared_ptr<PaymentRequest> request) 
        : id(request->getTransactionId()), request(request), 
          status(PaymentStatus::INITIATED), timestamp(getCurrentTimestamp()) {}

    void setStatus(PaymentStatus status) { this->status = status; }

    string getId() const { return id; }
    PaymentStatus getStatus() const { return status; }
    shared_ptr<PaymentRequest> getRequest() const { return request; }
};





enum class PaymentMethod {
    CREDIT_CARD,
    PAYPAL,
    UPI
};

enum class PaymentStatus {
    INITIATED,
    SUCCESSFUL,
    FAILED
};






class PaymentProcessorFactory {
public:
    static shared_ptr<PaymentProcessor> getProcessor(PaymentMethod method) {
        switch (method) {
            case PaymentMethod::CREDIT_CARD:
                return make_shared<CreditCardProcessor>();
            case PaymentMethod::UPI:
                return make_shared<UPIProcessor>();
            case PaymentMethod::PAYPAL:
                return make_shared<PayPalProcessor>();
            default:
                throw invalid_argument("Unsupported payment method");
        }
    }
};






class CustomerNotifier : public PaymentObserver {
public:
    void onTransactionUpdate(shared_ptr<Transaction> transaction) override {
        if (transaction->getStatus() == PaymentStatus::SUCCESSFUL) {
            cout << "--- CUSTOMER EMAIL ---" << endl;
            cout << "Your payment of " << transaction->getRequest()->getAmount() 
                 << " was successful. Transaction ID: " << transaction->getId() << endl;
            cout << "----------------------" << endl;
        }
    }
};



class MerchantNotifier : public PaymentObserver {
public:
    void onTransactionUpdate(shared_ptr<Transaction> transaction) override {
        cout << "--- MERCHANT NOTIFICATION ---" << endl;
        cout << "Transaction " << transaction->getId() << " status updated to: ";
        
        switch(transaction->getStatus()) {
            case PaymentStatus::INITIATED:
                cout << "INITIATED";
                break;
            case PaymentStatus::SUCCESSFUL:
                cout << "SUCCESSFUL";
                break;
            case PaymentStatus::FAILED:
                cout << "FAILED";
                break;
        }
        cout << endl;
        cout << "-----------------------------" << endl;
    }
};



class PaymentObserver {
public:
    virtual ~PaymentObserver() = default;
    virtual void onTransactionUpdate(shared_ptr<Transaction> transaction) = 0;
};










class AbstractPaymentProcessor : public PaymentProcessor {
private:
    static const int MAX_RETRIES = 3;

protected:
    virtual PaymentResponse doProcess(const PaymentRequest& request) = 0;

public:
    PaymentResponse processPayment(const PaymentRequest& request) override {
        int attempts = 0;
        PaymentResponse response(PaymentStatus::FAILED, "Initial state");
        
        do {
            response = doProcess(request);
            attempts++;
        } while (response.getStatus() == PaymentStatus::FAILED && attempts < MAX_RETRIES);
        
        return response;
    }
};





class CreditCardProcessor : public AbstractPaymentProcessor {
protected:
    PaymentResponse doProcess(const PaymentRequest& request) override {
        cout << "Processing credit card payment of amount " << request.getAmount() 
             << " " << request.getCurrency() << endl;
        // Simulate interaction with Visa/Mastercard network
        return PaymentResponse(PaymentStatus::SUCCESSFUL, "Credit Card payment successful.");
    }
};




class PaymentProcessor {
public:
    virtual ~PaymentProcessor() = default;
    virtual PaymentResponse processPayment(const PaymentRequest& request) = 0;
};



class PayPalProcessor : public AbstractPaymentProcessor {
protected:
    PaymentResponse doProcess(const PaymentRequest& request) override {
        cout << "Redirecting to PayPal for transaction " << request.getTransactionId() << endl;
        // Simulate PayPal API interaction
        return PaymentResponse(PaymentStatus::SUCCESSFUL, "Paypal payment successful.");
    }
};



class UPIProcessor : public AbstractPaymentProcessor {
protected:
    PaymentResponse doProcess(const PaymentRequest& request) override {
        cout << "Processing UPI payment of " << request.getAmount() 
             << " " << request.getCurrency() << endl;
        return PaymentResponse(PaymentStatus::SUCCESSFUL, "UPI payment successful.");
    }
};












int main() {
    // 1. Setup the gateway facade
    PaymentGatewayService* paymentGateway = PaymentGatewayService::getInstance();

    // 2. Register observers to be notified of transaction events
    paymentGateway->addObserver(make_shared<MerchantNotifier>());
    paymentGateway->addObserver(make_shared<CustomerNotifier>());

    cout << "----------- SCENARIO 1: Successful Credit Card Payment -----------" << endl;
    // a. Merchant's backend creates a payment request
    PaymentRequest ccRequest = PaymentRequest::Builder()
        .setPayerId("U-123")
        .setAmount(150.75)
        .setCurrency("INR")
        .setPaymentMethod(PaymentMethod::CREDIT_CARD)
        .setPaymentDetails({{"cardNumber", "1234..."}})
        .build();

    // b. Merchant's backend sends it to the facade
    paymentGateway->processPayment(ccRequest);

    cout << endl << "----------- SCENARIO 2: Successful PayPal Payment -----------" << endl;
    PaymentRequest paypalRequest = PaymentRequest::Builder()
        .setPayerId("U-456")
        .setAmount(88.50)
        .setCurrency("USD")
        .setPaymentMethod(PaymentMethod::PAYPAL)
        .setPaymentDetails({{"email", "customer@example.com"}})
        .build();

    paymentGateway->processPayment(paypalRequest);

    return 0;
}














class PaymentGatewayService {
private:
    static PaymentGatewayService* instance;
    static mutex instanceMutex;
    vector<shared_ptr<PaymentObserver>> observers;

    PaymentGatewayService() {}

    void notifyObservers(shared_ptr<Transaction> transaction) {
        for (const auto& observer : observers) {
            observer->onTransactionUpdate(transaction);
        }
    }

public:
    static PaymentGatewayService* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new PaymentGatewayService();
        }
        return instance;
    }

    void addObserver(shared_ptr<PaymentObserver> observer) {
        observers.push_back(observer);
    }

    void removeObserver(shared_ptr<PaymentObserver> observer) {
        observers.erase(
            remove(observers.begin(), observers.end(), observer),
            observers.end()
        );
    }

    shared_ptr<Transaction> processPayment(const PaymentRequest& request) {
        shared_ptr<PaymentRequest> requestPtr = make_shared<PaymentRequest>(request);
        shared_ptr<Transaction> transaction = make_shared<Transaction>(requestPtr);
        
        try {
            shared_ptr<PaymentProcessor> processor = PaymentProcessorFactory::getProcessor(request.getPaymentMethod());
            PaymentResponse response = processor->processPayment(request);
            transaction->setStatus(response.getStatus());
        } catch (const exception& e) {
            cerr << "Payment processing failed: " << e.what() << endl;
            transaction->setStatus(PaymentStatus::FAILED);
        }
        
        notifyObservers(transaction);
        return transaction;
    }
};

// Static member definitions (required for linking)
PaymentGatewayService* PaymentGatewayService::instance = nullptr;
mutex PaymentGatewayService::instanceMutex;





















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































