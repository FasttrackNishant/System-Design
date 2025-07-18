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
        double total = 0.0;
        for (size_t i = 0; i < prices.size(); ++i)
        {
            total += quantities[i] * prices[i];
        }
        return total;
    }

    void pay(const std::string &paymentType, const std::string &securityCode)
    {
        if (paymentType == "debit")
        {
            std::cout << "Processing debit payment type\n";
            std::cout << "Verifying security code: " << securityCode << std::endl;
            status = "paid";
        }
        else if (paymentType == "credit")
        {
            std::cout << "Processing credit payment type\n";
            std::cout << "Verifying security code: " << securityCode << std::endl;
            status = "paid";
        }
        else
        {
            throw std::runtime_error("Unknown payment type: " + paymentType);
        }
    }
};

int main()
{
    Order order;
    order.addItem("Keyboard", 1, 50.0);
    order.addItem("SSD", 1, 150.0);
    order.addItem("USB cable", 2, 5.0);

    std::cout << order.totalPrice() << std::endl;
    order.pay("debit", "0372846");

    return 0;
}
