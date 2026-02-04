class GiftWrapDecorator : ProductDecorator
{
    private const double GIFT_WRAP_COST = 5.00;

    public GiftWrapDecorator(Product product) : base(product) { }

    public override double GetPrice()
    {
        return base.GetPrice() + GIFT_WRAP_COST;
    }

    public override string GetDescription()
    {
        return base.GetDescription() + " (Gift Wrapped)";
    }
}



abstract class ProductDecorator : Product
{
    protected Product decoratedProduct;

    public ProductDecorator(Product product)
    {
        decoratedProduct = product;
    }

    public override string GetId() { return decoratedProduct.GetId(); }
    public override string GetName() { return decoratedProduct.GetName(); }
    public override double GetPrice() { return decoratedProduct.GetPrice(); }
    public override string GetDescription() { return decoratedProduct.GetDescription(); }
    public override ProductCategory GetCategory() { return decoratedProduct.GetCategory(); }
}









enum OrderStatus
{
    PENDING_PAYMENT,
    PLACED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}


enum ProductCategory
{
    ELECTRONICS,
    BOOKS,
    CLOTHING,
    HOME_GOODS,
    GROCERY
}





class OutOfStockException : Exception
{
    public OutOfStockException(string message) : base(message) { }
}








class Account
{
    private readonly string username;
    private readonly string password;
    private readonly ShoppingCart cart;

    public Account(string username, string password)
    {
        this.username = username;
        this.password = password;
        this.cart = new ShoppingCart();
    }

    public ShoppingCart GetCart() { return cart; }
}




class Address
{
    private readonly string street;
    private readonly string city;
    private readonly string state;
    private readonly string zipCode;

    public Address(string street, string city, string state, string zipCode)
    {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    public override string ToString()
    {
        return $"{street}, {city}, {state} {zipCode}";
    }
}



class CartItem
{
    private readonly Product product;
    private int quantity;

    public CartItem(Product product, int quantity)
    {
        this.product = product;
        this.quantity = quantity;
    }

    public Product GetProduct() { return product; }
    public int GetQuantity() { return quantity; }
    public void IncrementQuantity(int amount) { quantity += amount; }
    public double GetPrice() { return product.GetPrice() * quantity; }
}



class Customer : IOrderObserver
{
    private readonly string id;
    private readonly string name;
    private readonly string email;
    private readonly Account account;
    private Address shippingAddress;

    public Customer(string name, string email, string password, Address shippingAddress)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
        this.account = new Account(email, password);
        this.shippingAddress = shippingAddress;
    }

    public void Update(Order order)
    {
        Console.WriteLine($"[Notification for {name}]: Your order #{order.GetId()} status has been updated to: {order.GetStatus()}.");
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public Account GetAccount() { return account; }
    public Address GetShippingAddress() { return shippingAddress; }
    public void SetShippingAddress(Address address) { shippingAddress = address; }
}






class Order : Subject
{
    private readonly string id;
    private readonly Customer customer;
    private readonly List<OrderLineItem> items;
    private readonly Address shippingAddress;
    private readonly double totalAmount;
    private readonly DateTime orderDate;
    private OrderStatus status;
    private IOrderState currentState;

    public Order(Customer customer, List<OrderLineItem> items, Address shippingAddress, double totalAmount)
    {
        this.id = Guid.NewGuid().ToString().Substring(0, 8);
        this.customer = customer;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.orderDate = DateTime.Now;
        this.status = OrderStatus.PLACED;
        this.currentState = new PlacedState();
        AddObserver(customer);
    }

    public void ShipOrder() { currentState.Ship(this); }
    public void DeliverOrder() { currentState.Deliver(this); }
    public void CancelOrder() { currentState.Cancel(this); }

    public string GetId() { return id; }
    public OrderStatus GetStatus() { return status; }
    public void SetState(IOrderState state) { currentState = state; }
    public void SetStatus(OrderStatus status)
    {
        this.status = status;
        NotifyObservers(this);
    }
    public List<OrderLineItem> GetItems() { return items; }
}



class OrderLineItem
{
    private readonly string productId;
    private readonly string productName;
    private readonly int quantity;
    private readonly double priceAtPurchase;

    public OrderLineItem(string productId, string productName, int quantity, double priceAtPurchase)
    {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public string GetProductId() { return productId; }
    public int GetQuantity() { return quantity; }
}





abstract class Product
{
    protected string id;
    protected string name;
    protected string description;
    protected double price;
    protected ProductCategory category;

    public abstract string GetId();
    public abstract string GetName();
    public abstract string GetDescription();
    public abstract double GetPrice();
    public abstract ProductCategory GetCategory();
}

class BaseProduct : Product
{
    public BaseProduct(string id, string name, string description, double price, ProductCategory category)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public override string GetId() { return id; }
    public override string GetName() { return name; }
    public override string GetDescription() { return description; }
    public override double GetPrice() { return price; }
    public override ProductCategory GetCategory() { return category; }
}

class ProductBuilder
{
    private readonly string name;
    private readonly double price;
    private string description = "";
    private ProductCategory category;

    public ProductBuilder(string name, double price)
    {
        this.name = name;
        this.price = price;
    }

    public ProductBuilder WithDescription(string description)
    {
        this.description = description;
        return this;
    }

    public ProductBuilder WithCategory(ProductCategory category)
    {
        this.category = category;
        return this;
    }

    public Product Build()
    {
        return new BaseProduct(Guid.NewGuid().ToString(), name, description, price, category);
    }
}


class ShoppingCart
{
    private readonly Dictionary<string, CartItem> items = new Dictionary<string, CartItem>();

    public void AddItem(Product product, int quantity)
    {
        if (items.ContainsKey(product.GetId()))
        {
            items[product.GetId()].IncrementQuantity(quantity);
        }
        else
        {
            items[product.GetId()] = new CartItem(product, quantity);
        }
    }

    public void RemoveItem(string productId)
    {
        items.Remove(productId);
    }

    public Dictionary<string, CartItem> GetItems()
    {
        return new Dictionary<string, CartItem>(items);
    }

    public double CalculateTotal()
    {
        return items.Values.Sum(item => item.GetPrice());
    }

    public void ClearCart()
    {
        items.Clear();
    }
}











interface IOrderObserver
{
    void Update(Order order);
}



abstract class Subject
{
    private readonly List<IOrderObserver> observers = new List<IOrderObserver>();

    public void AddObserver(IOrderObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IOrderObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers(Order order)
    {
        foreach (var observer in observers)
        {
            observer.Update(order);
        }
    }
}









class InventoryService
{
    private readonly Dictionary<string, int> stock = new Dictionary<string, int>();
    private readonly object lockObj = new object();

    public void AddStock(Product product, int quantity)
    {
        lock (lockObj)
        {
            if (!stock.ContainsKey(product.GetId()))
            {
                stock[product.GetId()] = 0;
            }
            stock[product.GetId()] += quantity;
        }
    }

    public void UpdateStockForOrder(List<OrderLineItem> items)
    {
        lock (lockObj)
        {
            // First, check if all items are in stock
            foreach (var item in items)
            {
                if (!stock.ContainsKey(item.GetProductId()) || stock[item.GetProductId()] < item.GetQuantity())
                {
                    throw new OutOfStockException($"Not enough stock for product ID: {item.GetProductId()}");
                }
            }

            // If all checks pass, deduct the stock
            foreach (var item in items)
            {
                stock[item.GetProductId()] -= item.GetQuantity();
            }
        }
    }
}



class OrderService
{
    private readonly InventoryService inventoryService;

    public OrderService(InventoryService inventoryService)
    {
        this.inventoryService = inventoryService;
    }

    public Order CreateOrder(Customer customer, ShoppingCart cart)
    {
        var orderItems = cart.GetItems().Values
            .Select(cartItem => new OrderLineItem(
                cartItem.GetProduct().GetId(),
                cartItem.GetProduct().GetName(),
                cartItem.GetQuantity(),
                cartItem.GetProduct().GetPrice()))
            .ToList();

        inventoryService.UpdateStockForOrder(orderItems);

        return new Order(customer, orderItems, customer.GetShippingAddress(), cart.CalculateTotal());
    }
}



class PaymentService
{
    public bool ProcessPayment(IPaymentStrategy strategy, double amount)
    {
        return strategy.Pay(amount);
    }
}



class SearchService
{
    private readonly ICollection<Product> productCatalog;

    public SearchService(ICollection<Product> productCatalog)
    {
        this.productCatalog = productCatalog;
    }

    public List<Product> SearchByName(string name)
    {
        return productCatalog
            .Where(p => p.GetName().ToLower().Contains(name.ToLower()))
            .ToList();
    }

    public List<Product> SearchByCategory(ProductCategory category)
    {
        return productCatalog
            .Where(p => p.GetCategory() == category)
            .ToList();
    }
}








class CancelledState : IOrderState
{
    public void Ship(Order order)
    {
        Console.WriteLine("Cannot ship a cancelled order.");
    }

    public void Deliver(Order order)
    {
        Console.WriteLine("Cannot deliver a cancelled order.");
    }

    public void Cancel(Order order)
    {
        Console.WriteLine("Order is already cancelled.");
    }
}




class DeliveredState : IOrderState
{
    public void Ship(Order order)
    {
        Console.WriteLine("Order already delivered.");
    }

    public void Deliver(Order order)
    {
        Console.WriteLine("Order already delivered.");
    }

    public void Cancel(Order order)
    {
        Console.WriteLine("Cannot cancel a delivered order.");
    }
}



interface IOrderState
{
    void Ship(Order order);
    void Deliver(Order order);
    void Cancel(Order order);
}



class PlacedState : IOrderState
{
    public void Ship(Order order)
    {
        Console.WriteLine($"Shipping order {order.GetId()}");
        order.SetStatus(OrderStatus.SHIPPED);
        order.SetState(new ShippedState());
    }

    public void Deliver(Order order)
    {
        Console.WriteLine("Cannot deliver an order that has not been shipped.");
    }

    public void Cancel(Order order)
    {
        Console.WriteLine($"Cancelling order {order.GetId()}");
        order.SetStatus(OrderStatus.CANCELLED);
        order.SetState(new CancelledState());
    }
}



class ShippedState : IOrderState
{
    public void Ship(Order order)
    {
        Console.WriteLine("Order is already shipped.");
    }

    public void Deliver(Order order)
    {
        Console.WriteLine($"Delivering order {order.GetId()}");
        order.SetStatus(OrderStatus.DELIVERED);
        order.SetState(new DeliveredState());
    }

    public void Cancel(Order order)
    {
        Console.WriteLine("Cannot cancel a shipped order.");
    }
}






class CreditCardPaymentStrategy : IPaymentStrategy
{
    private readonly string cardNumber;

    public CreditCardPaymentStrategy(string cardNumber)
    {
        this.cardNumber = cardNumber;
    }

    public bool Pay(double amount)
    {
        Console.WriteLine($"Processing credit card payment of ${amount:F2} with card {cardNumber}.");
        return true;
    }
}


interface IPaymentStrategy
{
    bool Pay(double amount);
}



class UPIPaymentStrategy : IPaymentStrategy
{
    private readonly string upiId;

    public UPIPaymentStrategy(string upiId)
    {
        this.upiId = upiId;
    }

    public bool Pay(double amount)
    {
        Console.WriteLine($"Processing UPI payment of ${amount:F2} with upi id {upiId}.");
        return true;
    }
}







using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class OnlineShoppingDemo
{
    public static void Main(string[] args)
    {
        // System Setup (Singleton and Services)
        var system = OnlineShoppingSystem.GetInstance();

        // Create and Add Products to Catalog (Builder Pattern)
        var laptop = new ProductBuilder("Dell XPS 15", 1499.99)
            .WithDescription("A powerful and sleek laptop.")
            .WithCategory(ProductCategory.ELECTRONICS)
            .Build();

        var book = new ProductBuilder("The Pragmatic Programmer", 45.50)
            .WithDescription("A classic book for software developers.")
            .WithCategory(ProductCategory.BOOKS)
            .Build();

        system.AddProduct(laptop, 10); // 10 laptops in stock
        system.AddProduct(book, 50);   // 50 books in stock

        // Register a Customer
        var aliceAddress = new Address("123 Main St", "Anytown", "CA", "12345");
        var alice = system.RegisterCustomer("Alice", "alice@example.com", "password123", aliceAddress);

        // Alice Shops
        Console.WriteLine("--- Alice starts shopping ---");

        // Alice adds a laptop to her cart
        system.AddToCart(alice.GetId(), laptop.GetId(), 1);
        Console.WriteLine("Alice added a laptop to her cart.");

        // Alice decides to gift-wrap the book (Decorator Pattern)
        var giftWrappedBook = new GiftWrapDecorator(book);
        system.AddToCart(alice.GetId(), giftWrappedBook.GetId(), 1);
        Console.WriteLine($"Alice added a gift-wrapped book. Original price: ${book.GetPrice():F2}, New price: ${giftWrappedBook.GetPrice():F2}");

        var aliceCart = system.GetCustomerCart(alice.GetId());
        Console.WriteLine($"Alice's cart total: ${aliceCart.CalculateTotal():F2}");

        // Alice Checks Out
        Console.WriteLine("\n--- Alice proceeds to checkout ---");
        var aliceOrder = system.PlaceOrder(alice.GetId(), new CreditCardPaymentStrategy("1234-5678-9876-5432"));
        if (aliceOrder == null)
        {
            Console.WriteLine("Order placement failed.");
            return;
        }

        Console.WriteLine($"Order #{aliceOrder.GetId()} placed successfully for Alice.");

        // Order State and Notifications (State, Observer Patterns)
        Console.WriteLine("\n--- Order processing starts ---");

        // The warehouse ships the order
        aliceOrder.ShipOrder(); // This will trigger a notification to Alice

        // The delivery service marks the order as delivered
        aliceOrder.DeliverOrder(); // This will also trigger a notification

        // Try to cancel a delivered order (State pattern prevents this)
        aliceOrder.CancelOrder();

        Console.WriteLine("\n--- Out of Stock Scenario ---");
        var bob = system.RegisterCustomer("Bob", "bob@example.com", "pass123", aliceAddress);

        // Bob tries to buy 15 laptops, but only 9 are left (1 was bought by Alice)
        system.AddToCart(bob.GetId(), laptop.GetId(), 15);

        var bobOrder = system.PlaceOrder(bob.GetId(), new UPIPaymentStrategy("testupi@hdfc"));
        if (bobOrder == null)
        {
            Console.WriteLine("Bob's order was correctly prevented due to insufficient stock.");
        }
    }
}









class OnlineShoppingSystem
{
    private static volatile OnlineShoppingSystem instance;
    private static readonly object syncRoot = new object();

    private readonly Dictionary<string, Product> products = new Dictionary<string, Product>();
    private readonly Dictionary<string, Customer> customers = new Dictionary<string, Customer>();
    private readonly Dictionary<string, Order> orders = new Dictionary<string, Order>();

    private readonly InventoryService inventoryService;
    private readonly PaymentService paymentService;
    private readonly OrderService orderService;
    private readonly SearchService searchService;

    private OnlineShoppingSystem()
    {
        inventoryService = new InventoryService();
        paymentService = new PaymentService();
        orderService = new OrderService(inventoryService);
        searchService = new SearchService(products.Values);
    }

    public static OnlineShoppingSystem GetInstance()
    {
        if (instance == null)
        {
            lock (syncRoot)
            {
                if (instance == null)
                {
                    instance = new OnlineShoppingSystem();
                }
            }
        }
        return instance;
    }

    public void AddProduct(Product product, int initialStock)
    {
        products[product.GetId()] = product;
        inventoryService.AddStock(product, initialStock);
    }

    public Customer RegisterCustomer(string name, string email, string password, Address address)
    {
        var customer = new Customer(name, email, password, address);
        customers[customer.GetId()] = customer;
        return customer;
    }

    public void AddToCart(string customerId, string productId, int quantity)
    {
        var customer = customers[customerId];
        var product = products[productId];
        customer.GetAccount().GetCart().AddItem(product, quantity);
    }

    public ShoppingCart GetCustomerCart(string customerId)
    {
        var customer = customers[customerId];
        return customer.GetAccount().GetCart();
    }

    public List<Product> SearchProducts(string name)
    {
        return searchService.SearchByName(name);
    }

    public Order PlaceOrder(string customerId, IPaymentStrategy paymentStrategy)
    {
        var customer = customers[customerId];
        var cart = customer.GetAccount().GetCart();

        if (!cart.GetItems().Any())
        {
            Console.WriteLine("Cannot place an order with an empty cart.");
            return null;
        }

        // 1. Process payment
        bool paymentSuccess = paymentService.ProcessPayment(paymentStrategy, cart.CalculateTotal());
        if (!paymentSuccess)
        {
            Console.WriteLine("Payment failed. Please try again.");
            return null;
        }

        // 2. Create order and update inventory
        try
        {
            var order = orderService.CreateOrder(customer, cart);
            orders[order.GetId()] = order;

            // 3. Clear the cart
            cart.ClearCart();

            return order;
        }
        catch (Exception e)
        {
            Console.WriteLine($"Order placement failed: {e.Message}");
            return null;
        }
    }
}



































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































