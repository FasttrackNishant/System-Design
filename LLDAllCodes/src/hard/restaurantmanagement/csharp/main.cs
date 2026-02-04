interface ICommand
{
    void Execute();
}





class PrepareOrderCommand : ICommand
{
    private readonly Order order;
    private readonly Chef chef;

    public PrepareOrderCommand(Order order, Chef chef)
    {
        this.order = order;
        this.chef = chef;
    }

    public void Execute()
    {
        chef.PrepareOrder(order);
    }
}




class ServeOrderCommand : ICommand
{
    private readonly Order order;
    private readonly Waiter waiter;

    public ServeOrderCommand(Order order, Waiter waiter)
    {
        this.order = order;
        this.waiter = waiter;
    }

    public void Execute()
    {
        waiter.ServeOrder(order);
    }
}



abstract class BillDecorator : IBillComponent
{
    protected IBillComponent wrapped;

    public BillDecorator(IBillComponent component)
    {
        this.wrapped = component;
    }

    public virtual double CalculateTotal()
    {
        return wrapped.CalculateTotal();
    }

    public virtual string GetDescription()
    {
        return wrapped.GetDescription();
    }
}




class ServiceChargeDecorator : BillDecorator
{
    private readonly double serviceCharge;

    public ServiceChargeDecorator(IBillComponent component, double charge) : base(component)
    {
        this.serviceCharge = charge;
    }

    public override double CalculateTotal()
    {
        return base.CalculateTotal() + serviceCharge;
    }

    public override string GetDescription()
    {
        return base.GetDescription() + ", Service Charge";
    }
}




class TaxDecorator : BillDecorator
{
    private readonly double taxRate;

    public TaxDecorator(IBillComponent component, double taxRate) : base(component)
    {
        this.taxRate = taxRate;
    }

    public override double CalculateTotal()
    {
        return base.CalculateTotal() * (1 + taxRate);
    }

    public override string GetDescription()
    {
        return base.GetDescription() + $", Tax @{taxRate * 100}%";
    }
}





enum TableStatus
{
    AVAILABLE,
    OCCUPIED,
    RESERVED
}







class BaseBill : IBillComponent
{
    private readonly Order order;

    public BaseBill(Order order)
    {
        this.order = order;
    }

    public double CalculateTotal() => order.GetTotalPrice();
    public string GetDescription() => "Order Items";
}




class Bill
{
    private readonly IBillComponent component;

    public Bill(IBillComponent component)
    {
        this.component = component;
    }

    public void PrintBill()
    {
        Console.WriteLine("\n--- BILL ---");
        Console.WriteLine($"Description: {component.GetDescription()}");
        Console.WriteLine($"Total: ${component.CalculateTotal():F2}");
        Console.WriteLine("------------");
    }
}




class Chef : Staff
{
    public Chef(string id, string name) : base(id, name) { }

    public void PrepareOrder(Order order)
    {
        Console.WriteLine($"Chef {name} received order {order.GetOrderId()} and is starting preparation.");
        foreach (var item in order.GetOrderItems())
        {
            item.ChangeState(new PreparingState());
        }
    }
}




interface IBillComponent
{
    double CalculateTotal();
    string GetDescription();
}




class Menu
{
    private readonly Dictionary<string, MenuItem> items = new Dictionary<string, MenuItem>();

    public void AddItem(MenuItem item)
    {
        items[item.GetId()] = item;
    }

    public MenuItem GetItem(string id)
    {
        if (!items.TryGetValue(id, out MenuItem item))
        {
            throw new ArgumentException($"Menu item with ID {id} not found.");
        }
        return item;
    }
}





class MenuItem
{
    private readonly string id;
    private readonly string name;
    private readonly double price;

    public MenuItem(string id, string name, double price)
    {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public string GetId() => id;
    public string GetName() => name;
    public double GetPrice() => price;
}





class Order
{
    private readonly int orderId;
    private readonly int tableId;
    private readonly List<OrderItem> items = new List<OrderItem>();

    public Order(int orderId, int tableId)
    {
        this.orderId = orderId;
        this.tableId = tableId;
    }

    public void AddItem(OrderItem item)
    {
        items.Add(item);
    }

    public double GetTotalPrice()
    {
        return items.Sum(item => item.GetMenuItem().GetPrice());
    }

    public int GetOrderId() => orderId;
    public int GetTableId() => tableId;
    public List<OrderItem> GetOrderItems() => items;
}







class OrderItem
{
    private readonly MenuItem menuItem;
    private readonly Order order;
    private IOrderItemState state;
    private readonly List<IOrderObserver> observers = new List<IOrderObserver>();

    public OrderItem(MenuItem menuItem, Order order)
    {
        this.menuItem = menuItem;
        this.order = order;
        this.state = new OrderedState();
    }

    public void ChangeState(IOrderItemState newState)
    {
        this.state = newState;
        Console.WriteLine($"Item '{menuItem.GetName()}' state changed to: {newState.GetStatus()}");
    }

    public void NextState()
    {
        state.Next(this);
    }

    public void SetState(IOrderItemState state)
    {
        this.state = state;
    }

    public void AddObserver(IOrderObserver observer)
    {
        observers.Add(observer);
    }

    public void NotifyObservers()
    {
        foreach (var observer in observers.ToList())
        {
            observer.Update(this);
        }
    }

    public MenuItem GetMenuItem() => menuItem;
    public Order GetOrder() => order;
}










class Restaurant
{
    private static Restaurant instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, Waiter> waiters = new Dictionary<string, Waiter>();
    private readonly Dictionary<string, Chef> chefs = new Dictionary<string, Chef>();
    private readonly Dictionary<int, Table> tables = new Dictionary<int, Table>();
    private readonly Menu menu = new Menu();

    private Restaurant() { }

    public static Restaurant GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new Restaurant();
                }
            }
        }
        return instance;
    }

    public void AddWaiter(Waiter waiter) => waiters[waiter.GetId()] = waiter;
    public Waiter GetWaiter(string id) => waiters.TryGetValue(id, out Waiter waiter) ? waiter : null;

    public void AddChef(Chef chef) => chefs[chef.GetId()] = chef;
    public Chef GetChef(string id) => chefs.TryGetValue(id, out Chef chef) ? chef : null;

    public List<Chef> GetChefs() => chefs.Values.ToList();
    public List<Waiter> GetWaiters() => waiters.Values.ToList();

    public void AddTable(Table table) => tables[table.GetId()] = table;
    public Menu GetMenu() => menu;
}








abstract class Staff
{
    protected string id;
    protected string name;

    public Staff(string id, string name)
    {
        this.id = id;
        this.name = name;
    }

    public string GetId() => id;
    public string GetName() => name;
}






class Table
{
    private readonly int id;
    private readonly int capacity;
    private TableStatus status;

    public Table(int id, int capacity)
    {
        this.id = id;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

    public int GetId() => id;
    public int GetCapacity() => capacity;
    public TableStatus GetStatus() => status;
    public void SetStatus(TableStatus status) => this.status = status;
}






class Waiter : Staff, IOrderObserver
{
    public Waiter(string id, string name) : base(id, name) { }

    public void ServeOrder(Order order)
    {
        Console.WriteLine($"Waiter {name} is serving order {order.GetOrderId()}");
        foreach (var item in order.GetOrderItems())
        {
            item.ChangeState(new ServedState());
        }
    }

    public void Update(OrderItem item)
    {
        Console.WriteLine($">>> WAITER {name} NOTIFIED: Item '{item.GetMenuItem().GetName()}' " +
                         $"for table {item.GetOrder().GetTableId()} is READY FOR PICKUP.");
    }
}










interface IOrderObserver
{
    void Update(OrderItem item);
}





interface IOrderItemState
{
    void Next(OrderItem item);
    void Prev(OrderItem item);
    string GetStatus();
}



class OrderedState : IOrderItemState
{
    public void Next(OrderItem item)
    {
        item.SetState(new PreparingState());
    }

    public void Prev(OrderItem item)
    {
        Console.WriteLine("This is the initial state.");
    }

    public string GetStatus() => "ORDERED";
}







class PreparingState : IOrderItemState
{
    public void Next(OrderItem item)
    {
        item.SetState(new ReadyForPickupState());
    }

    public void Prev(OrderItem item)
    {
        item.SetState(new OrderedState());
    }

    public string GetStatus() => "PREPARING";
}




class ReadyForPickupState : IOrderItemState
{
    public void Next(OrderItem item)
    {
        item.NotifyObservers();
    }

    public void Prev(OrderItem item)
    {
        item.SetState(new PreparingState());
    }

    public string GetStatus() => "READY_FOR_PICKUP";
}






class ServedState : IOrderItemState
{
    public void Next(OrderItem item)
    {
        Console.WriteLine("This is the final state.");
    }

    public void Prev(OrderItem item)
    {
        Console.WriteLine("Cannot revert a served item.");
    }

    public string GetStatus() => "SERVED";
}






using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class RestaurantManagementSystemDemo
{
    public static void Main(string[] args)
    {
        Console.WriteLine("=== Initializing Restaurant System ===");
        RestaurantManagementSystemFacade rmsFacade = RestaurantManagementSystemFacade.GetInstance();

        Table table1 = rmsFacade.AddTable(1, 4);
        Chef chef1 = rmsFacade.AddChef("CHEF01", "Gordon");
        Waiter waiter1 = rmsFacade.AddWaiter("W01", "Alice");

        MenuItem pizza = rmsFacade.AddMenuItem("PIZZA01", "Margherita Pizza", 12.50);
        MenuItem pasta = rmsFacade.AddMenuItem("PASTA01", "Carbonara Pasta", 15.00);
        MenuItem coke = rmsFacade.AddMenuItem("DRINK01", "Coke", 2.50);
        Console.WriteLine("Initialization Complete.\n");

        Console.WriteLine("=== SCENARIO 1: Taking an order ===");
        Order order1 = rmsFacade.TakeOrder(table1.GetId(), waiter1.GetId(), 
            new List<string> { pizza.GetId(), coke.GetId() });
        Console.WriteLine($"Order taken successfully. Order ID: {order1.GetOrderId()}");

        Console.WriteLine("\n=== SCENARIO 2: Chef prepares, Waiter gets notified ===");
        rmsFacade.MarkItemsAsReady(order1.GetOrderId());
        rmsFacade.ServeOrder(waiter1.GetId(), order1.GetOrderId());

        Console.WriteLine("\n=== SCENARIO 3: Generating the bill ===");
        Bill finalBill = rmsFacade.GenerateBill(order1.GetOrderId());
        finalBill.PrintBill();
    }
}











class RestaurantManagementSystemFacade
{
    private static RestaurantManagementSystemFacade instance;
    private static readonly object lockObject = new object();
    private readonly Restaurant restaurant = Restaurant.GetInstance();
    private int orderIdCounter = 1;
    private readonly Dictionary<int, Order> orders = new Dictionary<int, Order>();

    private RestaurantManagementSystemFacade() { }

    public static RestaurantManagementSystemFacade GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new RestaurantManagementSystemFacade();
                }
            }
        }
        return instance;
    }

    public Table AddTable(int id, int capacity)
    {
        Table table = new Table(id, capacity);
        restaurant.AddTable(table);
        return table;
    }

    public Waiter AddWaiter(string id, string name)
    {
        Waiter waiter = new Waiter(id, name);
        restaurant.AddWaiter(waiter);
        return waiter;
    }

    public Chef AddChef(string id, string name)
    {
        Chef chef = new Chef(id, name);
        restaurant.AddChef(chef);
        return chef;
    }

    public MenuItem AddMenuItem(string id, string name, double price)
    {
        MenuItem item = new MenuItem(id, name, price);
        restaurant.GetMenu().AddItem(item);
        return item;
    }

    public Order TakeOrder(int tableId, string waiterId, List<string> menuItemIds)
    {
        Waiter waiter = restaurant.GetWaiter(waiterId);
        if (waiter == null)
        {
            throw new ArgumentException("Invalid waiter ID.");
        }

        var chefs = restaurant.GetChefs();
        if (!chefs.Any())
        {
            throw new InvalidOperationException("No chefs available.");
        }
        Chef chef = chefs.First();

        Order order = new Order(Interlocked.Increment(ref orderIdCounter) - 1, tableId);
        foreach (string itemId in menuItemIds)
        {
            MenuItem menuItem = restaurant.GetMenu().GetItem(itemId);
            OrderItem orderItem = new OrderItem(menuItem, order);
            orderItem.AddObserver(waiter);
            order.AddItem(orderItem);
        }

        ICommand prepareOrderCommand = new PrepareOrderCommand(order, chef);
        prepareOrderCommand.Execute();

        orders[order.GetOrderId()] = order;
        return order;
    }

    public void MarkItemsAsReady(int orderId)
    {
        Order order = orders[orderId];
        Console.WriteLine($"\nChef has finished preparing order {order.GetOrderId()}");

        foreach (var item in order.GetOrderItems())
        {
            item.NextState();
            item.NextState();
        }
    }

    public void ServeOrder(string waiterId, int orderId)
    {
        Order order = orders[orderId];
        Waiter waiter = restaurant.GetWaiter(waiterId);

        ICommand serveOrderCommand = new ServeOrderCommand(order, waiter);
        serveOrderCommand.Execute();
    }

    public Bill GenerateBill(int orderId)
    {
        Order order = orders[orderId];
        IBillComponent billComponent = new BaseBill(order);
        billComponent = new TaxDecorator(billComponent, 0.08);
        billComponent = new ServiceChargeDecorator(billComponent, 5.00);

        return new Bill(billComponent);
    }
}























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































