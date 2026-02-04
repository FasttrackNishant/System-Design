enum OrderStatus
{
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}







class Address
{
    private readonly string street;
    private readonly string city;
    private readonly string zipCode;
    private readonly double latitude;
    private readonly double longitude;

    public Address(string street, string city, string zipCode, double latitude, double longitude)
    {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public string GetCity()
    {
        return city;
    }

    public double DistanceTo(Address other)
    {
        double latDiff = latitude - other.latitude;
        double lonDiff = longitude - other.longitude;
        return Math.Sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    public override string ToString()
    {
        return $"{street}, {city}, {zipCode} @({latitude}, {longitude})";
    }
}






class Customer : User
{
    private readonly Address address;
    private readonly List<Order> orderHistory = new List<Order>();

    public Customer(string name, string phone, Address address) : base(name, phone)
    {
        this.address = address;
    }

    public void AddOrderToHistory(Order order)
    {
        orderHistory.Add(order);
    }

    public Address GetAddress()
    {
        return address;
    }

    public override void OnUpdate(Order order)
    {
        Console.WriteLine($"--- Notification for Customer {GetName()} ---");
        Console.WriteLine($"  Order {order.GetId()} is now {order.GetStatus()}.");
        Console.WriteLine("-------------------------------------\n");
    }
}









class DeliveryAgent : User
{
    private volatile bool isAvailable = true;
    private Address currentLocation;
    private readonly object locationLock = new object();

    public DeliveryAgent(string name, string phone, Address currentLocation) : base(name, phone)
    {
        this.currentLocation = currentLocation;
    }

    public void SetAvailable(bool available)
    {
        isAvailable = available;
    }

    public bool IsAvailableAgent()
    {
        return isAvailable;
    }

    public void SetCurrentLocation(Address currentLocation)
    {
        lock (locationLock)
        {
            this.currentLocation = currentLocation;
        }
    }

    public Address GetCurrentLocation()
    {
        lock (locationLock)
        {
            return currentLocation;
        }
    }

    public override void OnUpdate(Order order)
    {
        Console.WriteLine($"--- Notification for Delivery Agent {GetName()} ---");
        Console.WriteLine($"  Order {order.GetId()} update: Status is {order.GetStatus()}.");
        Console.WriteLine("-------------------------------------------\n");
    }
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
        return items.ContainsKey(id) ? items[id] : null;
    }

    public Dictionary<string, MenuItem> GetItems()
    {
        return new Dictionary<string, MenuItem>(items);
    }
}





class MenuItem
{
    private readonly string id;
    private readonly string name;
    private readonly double price;
    private bool available;

    public MenuItem(string id, string name, double price)
    {
        this.id = id;
        this.name = name;
        this.price = price;
        this.available = true;
    }

    public string GetId() { return id; }
    public void SetAvailable(bool available) { this.available = available; }
    public string GetName() { return name; }
    public double GetPrice() { return price; }

    public string GetMenuItem()
    {
        return $"Name: {name}, Price: {price}";
    }
}







class Order
{
    private readonly string id;
    private readonly Customer customer;
    private readonly Restaurant restaurant;
    private readonly List<OrderItem> items;
    private OrderStatus status;
    private DeliveryAgent deliveryAgent;
    private readonly List<IOrderObserver> observers = new List<IOrderObserver>();

    public Order(Customer customer, Restaurant restaurant, List<OrderItem> items)
    {
        this.id = Guid.NewGuid().ToString();
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;
        this.status = OrderStatus.PENDING;
        AddObserver(customer);
        AddObserver(restaurant);
    }

    public void AddObserver(IOrderObserver observer)
    {
        observers.Add(observer);
    }

    private void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.OnUpdate(this);
        }
    }

    public void SetStatus(OrderStatus newStatus)
    {
        if (status != newStatus)
        {
            status = newStatus;
            NotifyObservers();
        }
    }

    public bool Cancel()
    {
        if (status == OrderStatus.PENDING)
        {
            SetStatus(OrderStatus.CANCELLED);
            return true;
        }
        return false;
    }

    public void AssignDeliveryAgent(DeliveryAgent agent)
    {
        deliveryAgent = agent;
        AddObserver(agent);
        agent.SetAvailable(false);
    }

    public string GetId() { return id; }
    public OrderStatus GetStatus() { return status; }
    public Customer GetCustomer() { return customer; }
    public Restaurant GetRestaurant() { return restaurant; }
    public DeliveryAgent GetDeliveryAgent() { return deliveryAgent; }
}







class OrderItem
{
    private readonly MenuItem item;
    private readonly int quantity;

    public OrderItem(MenuItem item, int quantity)
    {
        this.item = item;
        this.quantity = quantity;
    }

    public MenuItem GetItem() { return item; }
    public int GetQuantity() { return quantity; }
}







class Restaurant : IOrderObserver
{
    private readonly string id;
    private readonly string name;
    private readonly Address address;
    private readonly Menu menu;

    public Restaurant(string name, Address address)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.address = address;
        this.menu = new Menu();
    }

    public void AddToMenu(MenuItem item)
    {
        menu.AddItem(item);
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public Address GetAddress() { return address; }
    public Menu GetMenu() { return menu; }

    public void OnUpdate(Order order)
    {
        Console.WriteLine($"--- Notification for Restaurant {GetName()} ---");
        Console.WriteLine($"  Order {order.GetId()} has been updated to {order.GetStatus()}.");
        Console.WriteLine("---------------------------------------\n");
    }
}







abstract class User : IOrderObserver
{
    protected readonly string id;
    protected readonly string name;
    protected readonly string phone;

    public User(string name, string phone)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.phone = phone;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }

    public abstract void OnUpdate(Order order);
}









interface IOrderObserver
{
    void OnUpdate(Order order);
}








interface IDeliveryAssignmentStrategy
{
    DeliveryAgent FindAgent(Order order, List<DeliveryAgent> agents);
}

class NearestAvailableAgentStrategy : IDeliveryAssignmentStrategy
{
    public DeliveryAgent FindAgent(Order order, List<DeliveryAgent> availableAgents)
    {
        Address restaurantAddress = order.GetRestaurant().GetAddress();
        Address customerAddress = order.GetCustomer().GetAddress();

        return availableAgents
            .Where(agent => agent.IsAvailableAgent())
            .OrderBy(agent => CalculateTotalDistance(agent, restaurantAddress, customerAddress))
            .FirstOrDefault();
    }

    private double CalculateTotalDistance(DeliveryAgent agent, Address restaurantAddress, Address customerAddress)
    {
        double agentToRestaurantDist = agent.GetCurrentLocation().DistanceTo(restaurantAddress);
        double restaurantToCustomerDist = restaurantAddress.DistanceTo(customerAddress);
        return agentToRestaurantDist + restaurantToCustomerDist;
    }
}








interface IRestaurantSearchStrategy
{
    List<Restaurant> Filter(List<Restaurant> allRestaurants);
}

class SearchByCityStrategy : IRestaurantSearchStrategy
{
    private readonly string city;

    public SearchByCityStrategy(string city)
    {
        this.city = city;
    }

    public List<Restaurant> Filter(List<Restaurant> allRestaurants)
    {
        return allRestaurants
            .Where(r => r.GetAddress().GetCity().Equals(city, StringComparison.OrdinalIgnoreCase))
            .ToList();
    }
}

class SearchByMenuKeywordStrategy : IRestaurantSearchStrategy
{
    private readonly string keyword;

    public SearchByMenuKeywordStrategy(string keyword)
    {
        this.keyword = keyword.ToLower();
    }

    public List<Restaurant> Filter(List<Restaurant> allRestaurants)
    {
        return allRestaurants
            .Where(r => r.GetMenu().GetItems().Values
                .Any(item => item.GetName().ToLower().Contains(keyword)))
            .ToList();
    }
}

class SearchByProximityStrategy : IRestaurantSearchStrategy
{
    private readonly Address userLocation;
    private readonly double maxDistance;

    public SearchByProximityStrategy(Address userLocation, double maxDistance)
    {
        this.userLocation = userLocation;
        this.maxDistance = maxDistance;
    }

    public List<Restaurant> Filter(List<Restaurant> allRestaurants)
    {
        return allRestaurants
            .Where(r => userLocation.DistanceTo(r.GetAddress()) <= maxDistance)
            .OrderBy(r => userLocation.DistanceTo(r.GetAddress()))
            .ToList();
    }
}










class FoodDeliveryService
{
    private static volatile FoodDeliveryService instance;
    private static readonly object lockObject = new object();
    private readonly ConcurrentDictionary<string, Customer> customers = new ConcurrentDictionary<string, Customer>();
    private readonly ConcurrentDictionary<string, Restaurant> restaurants = new ConcurrentDictionary<string, Restaurant>();
    private readonly ConcurrentDictionary<string, DeliveryAgent> deliveryAgents = new ConcurrentDictionary<string, DeliveryAgent>();
    private readonly ConcurrentDictionary<string, Order> orders = new ConcurrentDictionary<string, Order>();
    private IDeliveryAssignmentStrategy assignmentStrategy;

    private FoodDeliveryService() { }

    public static FoodDeliveryService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                    instance = new FoodDeliveryService();
            }
        }
        return instance;
    }

    public void SetAssignmentStrategy(IDeliveryAssignmentStrategy assignmentStrategy)
    {
        this.assignmentStrategy = assignmentStrategy;
    }

    public Customer RegisterCustomer(string name, string phone, Address address)
    {
        Customer customer = new Customer(name, phone, address);
        customers.TryAdd(customer.GetId(), customer);
        return customer;
    }

    public Restaurant RegisterRestaurant(string name, Address address)
    {
        Restaurant restaurant = new Restaurant(name, address);
        restaurants.TryAdd(restaurant.GetId(), restaurant);
        return restaurant;
    }

    public DeliveryAgent RegisterDeliveryAgent(string name, string phone, Address initialLocation)
    {
        DeliveryAgent deliveryAgent = new DeliveryAgent(name, phone, initialLocation);
        deliveryAgents.TryAdd(deliveryAgent.GetId(), deliveryAgent);
        return deliveryAgent;
    }

    public Order PlaceOrder(string customerId, string restaurantId, List<OrderItem> items)
    {
        if (!customers.TryGetValue(customerId, out Customer customer) ||
            !restaurants.TryGetValue(restaurantId, out Restaurant restaurant))
        {
            throw new KeyNotFoundException("Customer or Restaurant not found.");
        }

        Order order = new Order(customer, restaurant, items);
        orders.TryAdd(order.GetId(), order);
        customer.AddOrderToHistory(order);
        Console.WriteLine($"Order {order.GetId()} placed by {customer.GetName()} at {restaurant.GetName()}.");
        order.SetStatus(OrderStatus.PENDING);
        return order;
    }

    public void UpdateOrderStatus(string orderId, OrderStatus newStatus)
    {
        if (!orders.TryGetValue(orderId, out Order order))
        {
            throw new KeyNotFoundException("Order not found.");
        }

        order.SetStatus(newStatus);

        if (newStatus == OrderStatus.READY_FOR_PICKUP)
        {
            AssignDelivery(order);
        }
    }

    public void CancelOrder(string orderId)
    {
        if (!orders.TryGetValue(orderId, out Order order))
        {
            Console.WriteLine($"ERROR: Order with ID {orderId} not found.");
            return;
        }

        if (order.Cancel())
        {
            Console.WriteLine($"SUCCESS: Order {orderId} has been successfully canceled.");
        }
        else
        {
            Console.WriteLine($"FAILED: Order {orderId} could not be canceled. Its status is: {order.GetStatus()}");
        }
    }

    private void AssignDelivery(Order order)
    {
        List<DeliveryAgent> availableAgents = deliveryAgents.Values.ToList();

        DeliveryAgent agent = assignmentStrategy.FindAgent(order, availableAgents);
        if (agent != null)
        {
            order.AssignDeliveryAgent(agent);
            double distance = agent.GetCurrentLocation().DistanceTo(order.GetRestaurant().GetAddress());
            Console.WriteLine($"Agent {agent.GetName()} (dist: {distance:F2}) assigned to order {order.GetId()}.");
            order.SetStatus(OrderStatus.OUT_FOR_DELIVERY);
        }
        else
        {
            Console.WriteLine($"No available delivery agents found for order {order.GetId()}");
        }
    }

    public List<Restaurant> SearchRestaurants(List<IRestaurantSearchStrategy> strategies)
    {
        List<Restaurant> results = restaurants.Values.ToList();

        foreach (var strategy in strategies)
        {
            results = strategy.Filter(results);
        }

        return results;
    }

    public Menu GetRestaurantMenu(string restaurantId)
    {
        if (!restaurants.TryGetValue(restaurantId, out Restaurant restaurant))
        {
            throw new KeyNotFoundException($"Restaurant with ID {restaurantId} not found.");
        }
        return restaurant.GetMenu();
    }
}











using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;

public class FoodDeliveryServiceDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the system
        FoodDeliveryService service = FoodDeliveryService.GetInstance();
        service.SetAssignmentStrategy(new NearestAvailableAgentStrategy());

        // 2. Define Addresses
        Address aliceAddress = new Address("123 Maple St", "Springfield", "12345", 40.7128, -74.0060);
        Address pizzaAddress = new Address("456 Oak Ave", "Springfield", "12345", 40.7138, -74.0070);
        Address burgerAddress = new Address("789 Pine Ln", "Springfield", "12345", 40.7108, -74.0050);
        Address tacoAddress = new Address("101 Elm Ct", "Shelbyville", "54321", 41.7528, -75.0160);

        // 3. Register entities
        Customer alice = service.RegisterCustomer("Alice", "123-4567-890", aliceAddress);
        Restaurant pizzaPalace = service.RegisterRestaurant("Pizza Palace", pizzaAddress);
        Restaurant burgerBarn = service.RegisterRestaurant("Burger Barn", burgerAddress);
        Restaurant tacoTown = service.RegisterRestaurant("Taco Town", tacoAddress);
        service.RegisterDeliveryAgent("Bob", "321-4567-880", new Address("1 B", "Springfield", "12345", 40.71, -74.00));

        // 4. Setup menus
        pizzaPalace.AddToMenu(new MenuItem("P001", "Margherita Pizza", 12.99));
        pizzaPalace.AddToMenu(new MenuItem("P002", "Veggie Pizza", 11.99));
        burgerBarn.AddToMenu(new MenuItem("B001", "Classic Burger", 8.99));
        tacoTown.AddToMenu(new MenuItem("T001", "Crunchy Taco", 3.50));

        // 5. Demonstrate Search Functionality
        Console.WriteLine("\n--- 1. Searching for Restaurants ---");

        // (A) Search by City
        Console.WriteLine("\n(A) Restaurants in 'Springfield':");
        List<IRestaurantSearchStrategy> citySearch = new List<IRestaurantSearchStrategy> { new SearchByCityStrategy("Springfield") };
        List<Restaurant> springfieldRestaurants = service.SearchRestaurants(citySearch);
        foreach (var r in springfieldRestaurants)
        {
            Console.WriteLine($"  - {r.GetName()}");
        }

        // (B) Search for restaurants near Alice
        Console.WriteLine("\n(B) Restaurants near Alice (within 0.01 distance units):");
        List<IRestaurantSearchStrategy> proximitySearch = new List<IRestaurantSearchStrategy> { new SearchByProximityStrategy(aliceAddress, 0.01) };
        List<Restaurant> nearbyRestaurants = service.SearchRestaurants(proximitySearch);
        foreach (var r in nearbyRestaurants)
        {
            Console.WriteLine($"  - {r.GetName()} (Distance: {aliceAddress.DistanceTo(r.GetAddress()):F4})");
        }

        // (C) Search for restaurants that serve 'Pizza'
        Console.WriteLine("\n(C) Restaurants that serve 'Pizza':");
        List<IRestaurantSearchStrategy> menuSearch = new List<IRestaurantSearchStrategy> { new SearchByMenuKeywordStrategy("Pizza") };
        List<Restaurant> pizzaRestaurants = service.SearchRestaurants(menuSearch);
        foreach (var r in pizzaRestaurants)
        {
            Console.WriteLine($"  - {r.GetName()}");
        }

        // (D) Combined Search: Find restaurants near Alice that serve 'Burger'
        Console.WriteLine("\n(D) Burger joints near Alice:");
        List<IRestaurantSearchStrategy> combinedSearch = new List<IRestaurantSearchStrategy>
        {
            new SearchByProximityStrategy(aliceAddress, 0.01),
            new SearchByMenuKeywordStrategy("Burger")
        };
        List<Restaurant> burgerJointsNearAlice = service.SearchRestaurants(combinedSearch);
        foreach (var r in burgerJointsNearAlice)
        {
            Console.WriteLine($"  - {r.GetName()}");
        }

        // 6. Demonstrate Browsing a Menu
        Console.WriteLine("\n--- 2. Browsing a Menu ---");
        Console.WriteLine("\nMenu for 'Pizza Palace':");
        Menu pizzaMenu = service.GetRestaurantMenu(pizzaPalace.GetId());
        foreach (var item in pizzaMenu.GetItems().Values)
        {
            Console.WriteLine($"  - {item.GetName()}: ${item.GetPrice():F2}");
        }

        // 7. Alice places an order from a searched restaurant
        Console.WriteLine("\n--- 3. Placing an Order ---");
        if (pizzaRestaurants.Count > 0)
        {
            Restaurant chosenRestaurant = pizzaRestaurants[0];
            MenuItem chosenItem = chosenRestaurant.GetMenu().GetItem("P001");

            Console.WriteLine($"\nAlice is ordering '{chosenItem.GetName()}' from '{chosenRestaurant.GetName()}'.");
            var order = service.PlaceOrder(alice.GetId(), chosenRestaurant.GetId(), new List<OrderItem> { new OrderItem(chosenItem, 1) });

            Console.WriteLine("\n--- Restaurant starts preparing the order ---");
            service.UpdateOrderStatus(order.GetId(), OrderStatus.PREPARING);

            Console.WriteLine("\n--- Order is ready for pickup ---");
            Console.WriteLine("System will now find the nearest available delivery agent...");
            service.UpdateOrderStatus(order.GetId(), OrderStatus.READY_FOR_PICKUP);

            Console.WriteLine("\n--- Agent delivers the order ---");
            service.UpdateOrderStatus(order.GetId(), OrderStatus.DELIVERED);
        }
    }
}










































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































