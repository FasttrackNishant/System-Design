class BuyStockCommand : IOrderCommand
{
    private readonly Account account;
    private readonly Order order;
    private readonly StockExchange stockExchange;

    public BuyStockCommand(Account account, Order order)
    {
        this.account = account;
        this.order = order;
        this.stockExchange = StockExchange.GetInstance();
    }

    public void Execute()
    {
        double estimatedCost = order.GetQuantity() * order.GetPrice();
        if (order.GetOrderType() == OrderType.LIMIT && account.GetBalance() < estimatedCost)
        {
            throw new InsufficientFundsException("Not enough cash to place limit buy order.");
        }
        Console.WriteLine($"Placing BUY order {order.GetOrderId()} for {order.GetQuantity()} shares of {order.GetStock().GetSymbol()}.");
        stockExchange.PlaceBuyOrder(order);
    }
}


interface IOrderCommand
{
    void Execute();
}




class SellStockCommand : IOrderCommand
{
    private readonly Account account;
    private readonly Order order;
    private readonly StockExchange stockExchange;

    public SellStockCommand(Account account, Order order)
    {
        this.account = account;
        this.order = order;
        this.stockExchange = StockExchange.GetInstance();
    }

    public void Execute()
    {
        if (account.GetStockQuantity(order.GetStock().GetSymbol()) < order.GetQuantity())
        {
            throw new InsufficientStockException("Not enough stock to place sell order.");
        }
        Console.WriteLine($"Placing SELL order {order.GetOrderId()} for {order.GetQuantity()} shares of {order.GetStock().GetSymbol()}.");
        stockExchange.PlaceSellOrder(order);
    }
}







enum OrderStatus
{
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    FAILED
}





enum OrderType
{
    MARKET,
    LIMIT
}



enum TransactionType
{
    BUY,
    SELL
}







class InsufficientFundsException : Exception
{
    public InsufficientFundsException(string message) : base(message) { }
}

class InsufficientStockException : Exception
{
    public InsufficientStockException(string message) : base(message) { }
}









class Account
{
    private double balance;
    private readonly Dictionary<string, int> portfolio; // Stock symbol -> quantity
    private readonly object lockObj = new object();

    public Account(double initialCash)
    {
        this.balance = initialCash;
        this.portfolio = new Dictionary<string, int>();
    }

    public void Debit(double amount)
    {
        lock (lockObj)
        {
            if (balance < amount)
            {
                throw new InsufficientFundsException($"Insufficient funds to debit {amount}");
            }
            balance -= amount;
        }
    }

    public void Credit(double amount)
    {
        lock (lockObj)
        {
            balance += amount;
        }
    }

    public void AddStock(string symbol, int quantity)
    {
        lock (lockObj)
        {
            if (!portfolio.ContainsKey(symbol))
            {
                portfolio[symbol] = 0;
            }
            portfolio[symbol] += quantity;
        }
    }

    public void RemoveStock(string symbol, int quantity)
    {
        lock (lockObj)
        {
            int currentQuantity = portfolio.ContainsKey(symbol) ? portfolio[symbol] : 0;
            if (currentQuantity < quantity)
            {
                throw new InsufficientStockException($"Not enough {symbol} stock to sell.");
            }
            portfolio[symbol] = currentQuantity - quantity;
        }
    }

    public double GetBalance() { return balance; }
    public Dictionary<string, int> GetPortfolio() { return new Dictionary<string, int>(portfolio); }
    public int GetStockQuantity(string symbol) { return portfolio.ContainsKey(symbol) ? portfolio[symbol] : 0; }
}







class Order
{
    private readonly string orderId;
    private readonly User user;
    private readonly Stock stock;
    private readonly OrderType type;
    private readonly int quantity;
    private readonly double price; // Limit price for Limit orders
    private OrderStatus status;
    private readonly User owner;
    private IOrderState currentState;
    private readonly IExecutionStrategy executionStrategy;

    public Order(string orderId, User user, Stock stock, OrderType type, int quantity, 
                 double price, IExecutionStrategy strategy, User owner)
    {
        this.orderId = orderId;
        this.user = user;
        this.stock = stock;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.executionStrategy = strategy;
        this.owner = owner;
        this.currentState = new OpenState(); // Initial state
        this.status = OrderStatus.OPEN;
    }

    public void Cancel()
    {
        currentState.Cancel(this);
    }

    // Getters
    public string GetOrderId() { return orderId; }
    public User GetUser() { return user; }
    public Stock GetStock() { return stock; }
    public OrderType GetOrderType() { return type; }
    public int GetQuantity() { return quantity; }
    public double GetPrice() { return price; }
    public OrderStatus GetStatus() { return status; }
    public IExecutionStrategy GetExecutionStrategy() { return executionStrategy; }

    // Setters for state transitions
    public void SetState(IOrderState state)
    {
        this.currentState = state;
    }

    public void SetStatus(OrderStatus status)
    {
        this.status = status;
        NotifyOwner();
    }

    private void NotifyOwner()
    {
        if (owner != null)
        {
            owner.OrderStatusUpdate(this);
        }
    }
}






class OrderBuilder
{
    private User user;
    private Stock stock;
    private OrderType type;
    private TransactionType transactionType;
    private int quantity;
    private double price;

    public OrderBuilder ForUser(User user)
    {
        this.user = user;
        return this;
    }

    public OrderBuilder WithStock(Stock stock)
    {
        this.stock = stock;
        return this;
    }

    public OrderBuilder Buy(int quantity)
    {
        this.transactionType = TransactionType.BUY;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder Sell(int quantity)
    {
        this.transactionType = TransactionType.SELL;
        this.quantity = quantity;
        return this;
    }

    public OrderBuilder AtMarketPrice()
    {
        this.type = OrderType.MARKET;
        this.price = 0; // Not needed for market order
        return this;
    }

    public OrderBuilder WithLimit(double limitPrice)
    {
        this.type = OrderType.LIMIT;
        this.price = limitPrice;
        return this;
    }

    public Order Build()
    {
        IExecutionStrategy strategy = type == OrderType.MARKET ? 
            (IExecutionStrategy) new MarketOrderStrategy() : 
            (IExecutionStrategy) new LimitOrderStrategy(transactionType);
        
        return new Order(
            Guid.NewGuid().ToString(),
            user,
            stock,
            type,
            quantity,
            price,
            strategy,
            user
        );
    }
}





class Stock
{
    private readonly string symbol;
    private double price;
    private readonly List<IStockObserver> observers = new List<IStockObserver>();

    public Stock(string symbol, double initialPrice)
    {
        this.symbol = symbol;
        this.price = initialPrice;
    }

    public string GetSymbol() { return symbol; }
    public double GetPrice() { return price; }

    public void SetPrice(double newPrice)
    {
        if (this.price != newPrice)
        {
            this.price = newPrice;
            NotifyObservers();
        }
    }

    public void AddObserver(IStockObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IStockObserver observer)
    {
        observers.Remove(observer);
    }

    private void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.Update(this);
        }
    }
}






class User : IStockObserver
{
    private readonly string userId;
    private readonly string name;
    private readonly Account account;

    public User(string name, double initialCash)
    {
        this.userId = Guid.NewGuid().ToString();
        this.name = name;
        this.account = new Account(initialCash);
    }

    public string GetUserId() { return userId; }
    public string GetName() { return name; }
    public Account GetAccount() { return account; }

    public void Update(Stock stock)
    {
        Console.WriteLine($"[Notification for {name}] Stock {stock.GetSymbol()} price updated to: ${stock.GetPrice():F2}");
    }

    public void OrderStatusUpdate(Order order)
    {
        Console.WriteLine($"[Order Notification for {name}] Order {order.GetOrderId()} for {order.GetStock().GetSymbol()} is now {order.GetStatus()}.");
    }
}







interface IStockObserver
{
    void Update(Stock stock);
}



class CancelledState : IOrderState
{
    public void Handle(Order order)
    {
        Console.WriteLine("Order is cancelled.");
    }

    public void Cancel(Order order)
    {
        Console.WriteLine("Order is already cancelled.");
    }
}




class FilledState : IOrderState
{
    public void Handle(Order order)
    {
        Console.WriteLine("Order is already filled.");
    }

    public void Cancel(Order order)
    {
        Console.WriteLine("Cannot cancel a filled order.");
    }
}



interface IOrderState
{
    void Handle(Order order);
    void Cancel(Order order);
}





class OpenState : IOrderState
{
    public void Handle(Order order)
    {
        Console.WriteLine("Order is open and waiting for execution.");
    }

    public void Cancel(Order order)
    {
        order.SetStatus(OrderStatus.CANCELLED);
        order.SetState(new CancelledState());
        Console.WriteLine($"Order {order.GetOrderId()} has been cancelled.");
    }
}













interface IExecutionStrategy
{
    bool CanExecute(Order order, double marketPrice);
}




class LimitOrderStrategy : IExecutionStrategy
{
    private readonly TransactionType type;

    public LimitOrderStrategy(TransactionType type)
    {
        this.type = type;
    }

    public bool CanExecute(Order order, double marketPrice)
    {
        if (type == TransactionType.BUY)
        {
            // Buy if market price is less than or equal to limit price
            return marketPrice <= order.GetPrice();
        }
        else // SELL
        {
            // Sell if market price is greater than or equal to limit price
            return marketPrice >= order.GetPrice();
        }
    }
}




class MarketOrderStrategy : IExecutionStrategy
{
    public bool CanExecute(Order order, double marketPrice)
    {
        return true; // Market orders can always execute
    }
}








class StockBrokerageSystem
{
    private static volatile StockBrokerageSystem instance;
    private static readonly object syncRoot = new object();
    private readonly Dictionary<string, User> users;
    private readonly Dictionary<string, Stock> stocks;

    private StockBrokerageSystem()
    {
        this.users = new Dictionary<string, User>();
        this.stocks = new Dictionary<string, Stock>();
    }

    public static StockBrokerageSystem GetInstance()
    {
        if (instance == null)
        {
            lock (syncRoot)
            {
                if (instance == null)
                {
                    instance = new StockBrokerageSystem();
                }
            }
        }
        return instance;
    }

    public User RegisterUser(string name, double initialAmount)
    {
        var user = new User(name, initialAmount);
        users[user.GetUserId()] = user;
        return user;
    }

    public Stock AddStock(string symbol, double initialPrice)
    {
        var stock = new Stock(symbol, initialPrice);
        stocks[stock.GetSymbol()] = stock;
        return stock;
    }

    public void PlaceBuyOrder(Order order)
    {
        var user = order.GetUser();
        IOrderCommand command = new BuyStockCommand(user.GetAccount(), order);
        command.Execute();
    }

    public void PlaceSellOrder(Order order)
    {
        var user = order.GetUser();
        IOrderCommand command = new SellStockCommand(user.GetAccount(), order);
        command.Execute();
    }

    public void CancelOrder(Order order)
    {
        order.Cancel();
    }
}









using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

public class StockBrokerageSystemDemo
{
    public static void Main(string[] args)
    {
        // System Setup
        var system = StockBrokerageSystem.GetInstance();

        // Create Stocks
        var apple = system.AddStock("AAPL", 150.00);
        var google = system.AddStock("GOOG", 2800.00);

        // Create Members (Users)
        var alice = system.RegisterUser("Alice", 20000.00);
        var bob = system.RegisterUser("Bob", 25000.00);

        // Bob already owns some Apple stock
        bob.GetAccount().AddStock("AAPL", 50);

        // Members subscribe to stock notifications (Observer Pattern)
        apple.AddObserver(alice);
        google.AddObserver(alice);
        apple.AddObserver(bob);

        Console.WriteLine("--- Initial State ---");
        PrintAccountStatus(alice);
        PrintAccountStatus(bob);

        Console.WriteLine("\n--- Trading Simulation Starts ---\n");

        // SCENARIO 1: Limit Order Match
        Console.WriteLine("--- SCENARIO 1: Alice places a limit buy, Bob places a limit sell that matches ---");

        // Alice wants to buy 10 shares of AAPL if the price is $150.50 or less
        var aliceBuyOrder = new OrderBuilder()
            .ForUser(alice)
            .Buy(10)
            .WithStock(apple)
            .WithLimit(150.50)
            .Build();
        system.PlaceBuyOrder(aliceBuyOrder);

        // Bob wants to sell 20 of his shares if the price is $150.50 or more
        var bobSellOrder = new OrderBuilder()
            .ForUser(bob)
            .Sell(20)
            .WithStock(apple)
            .WithLimit(150.50)
            .Build();
        system.PlaceSellOrder(bobSellOrder);

        // The exchange will automatically match and execute this trade.
        Thread.Sleep(100); // Give time for notifications to print
        Console.WriteLine("\n--- Account Status After Trade 1 ---");
        PrintAccountStatus(alice);
        PrintAccountStatus(bob);

        // SCENARIO 2: Price Update triggers notifications
        Console.WriteLine("\n--- SCENARIO 2: Market price of GOOG changes ---");
        google.SetPrice(2850.00); // Alice will get a notification

        // SCENARIO 3: Order Cancellation (State Pattern)
        Console.WriteLine("\n--- SCENARIO 3: Alice places an order and then cancels it ---");
        var aliceCancelOrder = new OrderBuilder()
            .ForUser(alice)
            .Buy(5)
            .WithStock(google)
            .WithLimit(2700.00) // Price is too low, so it won't execute immediately
            .Build();
        system.PlaceBuyOrder(aliceCancelOrder);

        Console.WriteLine($"Order status before cancellation: {aliceCancelOrder.GetStatus()}");
        system.CancelOrder(aliceCancelOrder);
        Console.WriteLine($"Order status after cancellation attempt: {aliceCancelOrder.GetStatus()}");

        // Now try to cancel an already filled order
        Console.WriteLine("\n--- Trying to cancel an already FILLED order (State Pattern) ---");
        Console.WriteLine($"Bob's sell order status: {bobSellOrder.GetStatus()}");
        system.CancelOrder(bobSellOrder); // This should fail
        Console.WriteLine($"Bob's sell order status after cancel attempt: {bobSellOrder.GetStatus()}");
    }

    private static void PrintAccountStatus(User user)
    {
        var portfolio = user.GetAccount().GetPortfolio();
        var portfolioStr = string.Join(", ", portfolio.Select(kvp => $"{kvp.Key}: {kvp.Value}"));
        Console.WriteLine($"Member: {user.GetName()}, Cash: ${user.GetAccount().GetBalance():F2}, Portfolio: {{{portfolioStr}}}");
    }
}











class StockExchange
{
    private static volatile StockExchange instance;
    private static readonly object syncRoot = new object();
    private readonly Dictionary<string, List<Order>> buyOrders;
    private readonly Dictionary<string, List<Order>> sellOrders;
    private readonly object matchLock = new object();

    private StockExchange()
    {
        this.buyOrders = new Dictionary<string, List<Order>>();
        this.sellOrders = new Dictionary<string, List<Order>>();
    }

    public static StockExchange GetInstance()
    {
        if (instance == null)
        {
            lock (syncRoot)
            {
                if (instance == null)
                {
                    instance = new StockExchange();
                }
            }
        }
        return instance;
    }

    public void PlaceBuyOrder(Order order)
    {
        if (!buyOrders.ContainsKey(order.GetStock().GetSymbol()))
        {
            buyOrders[order.GetStock().GetSymbol()] = new List<Order>();
        }
        buyOrders[order.GetStock().GetSymbol()].Add(order);
        MatchOrders(order.GetStock());
    }

    public void PlaceSellOrder(Order order)
    {
        if (!sellOrders.ContainsKey(order.GetStock().GetSymbol()))
        {
            sellOrders[order.GetStock().GetSymbol()] = new List<Order>();
        }
        sellOrders[order.GetStock().GetSymbol()].Add(order);
        MatchOrders(order.GetStock());
    }

    private void MatchOrders(Stock stock)
    {
        lock (matchLock) // Critical section to prevent race conditions during matching
        {
            var buys = buyOrders.ContainsKey(stock.GetSymbol()) ? buyOrders[stock.GetSymbol()] : new List<Order>();
            var sells = sellOrders.ContainsKey(stock.GetSymbol()) ? sellOrders[stock.GetSymbol()] : new List<Order>();

            if (!buys.Any() || !sells.Any()) return;

            bool matchFound;
            do
            {
                matchFound = false;
                var bestBuy = FindBestBuy(buys);
                var bestSell = FindBestSell(sells);

                if (bestBuy != null && bestSell != null)
                {
                    double buyPrice = bestBuy.GetOrderType() == OrderType.MARKET ? stock.GetPrice() : bestBuy.GetPrice();
                    double sellPrice = bestSell.GetOrderType() == OrderType.MARKET ? stock.GetPrice() : bestSell.GetPrice();

                    if (buyPrice >= sellPrice)
                    {
                        ExecuteTrade(bestBuy, bestSell, sellPrice); // Trade at the seller's asking price
                        matchFound = true;
                    }
                }
            } while (matchFound);
        }
    }

    private void ExecuteTrade(Order buyOrder, Order sellOrder, double tradePrice)
    {
        Console.WriteLine($"--- Executing Trade for {buyOrder.GetStock().GetSymbol()} at ${tradePrice:F2} ---");

        var buyer = buyOrder.GetUser();
        var seller = sellOrder.GetUser();

        int tradeQuantity = Math.Min(buyOrder.GetQuantity(), sellOrder.GetQuantity());
        double totalCost = tradeQuantity * tradePrice;

        // Perform transaction
        buyer.GetAccount().Debit(totalCost);
        buyer.GetAccount().AddStock(buyOrder.GetStock().GetSymbol(), tradeQuantity);

        seller.GetAccount().Credit(totalCost);
        seller.GetAccount().RemoveStock(sellOrder.GetStock().GetSymbol(), tradeQuantity);

        // Update orders
        UpdateOrderStatus(buyOrder, tradeQuantity);
        UpdateOrderStatus(sellOrder, tradeQuantity);

        // Update stock's market price to last traded price
        buyOrder.GetStock().SetPrice(tradePrice);

        Console.WriteLine("--- Trade Complete ---");
    }

    private void UpdateOrderStatus(Order order, int quantityTraded)
    {
        // This is a simplified update logic. A real system would handle partial fills.
        order.SetStatus(OrderStatus.FILLED);
        order.SetState(new FilledState());
        string stockSymbol = order.GetStock().GetSymbol();
        
        // Remove from books
        if (buyOrders.ContainsKey(stockSymbol))
            buyOrders[stockSymbol].Remove(order);
        if (sellOrders.ContainsKey(stockSymbol))
            sellOrders[stockSymbol].Remove(order);
    }

    private Order FindBestBuy(List<Order> buys)
    {
        return buys
            .Where(o => o.GetStatus() == OrderStatus.OPEN)
            .OrderByDescending(o => o.GetPrice()) // Highest limit price is best
            .FirstOrDefault();
    }

    private Order FindBestSell(List<Order> sells)
    {
        return sells
            .Where(o => o.GetStatus() == OrderStatus.OPEN)
            .OrderBy(o => o.GetPrice()) // Lowest limit price is best
            .FirstOrDefault();
    }
}










































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































