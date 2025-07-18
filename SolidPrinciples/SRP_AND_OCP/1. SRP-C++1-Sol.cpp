#include <iostream>
#include <vector>
#include <string>

class Order
{
private:
    std::vector<std::string> items;
    std::vector<int> quantities;
    std::vector<double> prices;
    std::string status;

public:
    Order() : status("open") {}

    void addItem(const std::string &name, int quantity, double price)
    {
        items.push_back(name);
        quantities.push_back(quantity);
        prices.push_back(price);
    }

    double totalPrice() const
    {
        double total = 0;
        for (size_t i = 0; i < prices.size(); ++i)
        {
            total += quantities[i] * prices[i];
        }
        return total;
    }

    void setStatus(const std::string &newStatus)
    {
        status = newStatus;
    }

    const std::string &getStatus() const
    {
        return status;
    }
};

class PaymentProcessor
{
public:
    void pay(Order &order, const std::string &securityCode, std::string paymentType)
    {
        if (paymentType == "debit")
        {
            std::cout << "Processing debit payment type" << std::endl;
            std::cout << "Verifying security code: " << securityCode << std::endl;
            order.setStatus("paid");
        }
        else if (paymentType == "credit")
        {
            std::cout << "Processing credit payment type" << std::endl;
            std::cout << "Verifying security code: " << securityCode << std::endl;
            order.setStatus("paid");
        }
    }
};

int main()
{
    Order order;
    order.addItem("Keyboard", 1, 50);
    order.addItem("SSD", 1, 150);
    order.addItem("USB cable", 2, 5);

    std::cout << order.totalPrice() << std::endl;

    PaymentProcessor processor;
    processor.pay(order, "0372846", "debit");

    return 0;
}
s