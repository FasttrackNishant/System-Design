class Trip
{
    private readonly string id;
    private readonly Rider rider;
    private Driver driver;
    private readonly Location pickupLocation;
    private readonly Location dropoffLocation;
    private readonly double fare;
    private TripStatus status;
    private ITripState currentState;
    private readonly List<ITripObserver> observers = new List<ITripObserver>();

    public static int idCounter = 0;

    public Trip(TripBuilder builder)
    {
        id = builder.Id;
        rider = builder.Rider;
        driver = null;
        pickupLocation = builder.PickupLocation;
        dropoffLocation = builder.DropoffLocation;
        fare = builder.Fare;
        status = TripStatus.REQUESTED;
        currentState = new RequestedState();
    }

    public void AddObserver(ITripObserver observer)
    {
        observers.Add(observer);
    }

    private void NotifyObservers()
    {
        foreach (var obs in observers)
        {
            obs.OnUpdate(this);
        }
    }

    public void AssignDriver(Driver d)
    {
        currentState.Assign(this, d);
        AddObserver(d);
        NotifyObservers();
    }

    public void StartTrip()
    {
        currentState.Start(this);
        NotifyObservers();
    }

    public void EndTrip()
    {
        currentState.End(this);
        NotifyObservers();
    }

    // Getters
    public string Id => id;
    public Rider Rider => rider;
    public Driver Driver => driver;
    public Location PickupLocation => pickupLocation;
    public Location DropoffLocation => dropoffLocation;
    public double Fare => fare;
    public TripStatus Status => status;

    // Setters (internal, only to be called by State objects)
    public void SetState(ITripState state)
    {
        currentState = state;
    }

    public void SetStatus(TripStatus s)
    {
        status = s;
    }

    public void SetDriver(Driver d)
    {
        driver = d;
    }

    public override string ToString()
    {
        return $"Trip [id={id}, status={status}, fare=${fare:F2}]";
    }
}

class TripBuilder
{
    private readonly string id;
    private Rider rider;
    private Location pickupLocation;
    private Location dropoffLocation;
    private double fare;

    public TripBuilder()
    {
        id = $"trip_{++Trip.idCounter}";
    }

    public TripBuilder WithRider(Rider r)
    {
        rider = r;
        return this;
    }

    public TripBuilder WithPickupLocation(Location loc)
    {
        pickupLocation = loc;
        return this;
    }

    public TripBuilder WithDropoffLocation(Location loc)
    {
        dropoffLocation = loc;
        return this;
    }

    public TripBuilder WithFare(double f)
    {
        fare = f;
        return this;
    }

    public Trip Build()
    {
        if (rider == null || pickupLocation == null || dropoffLocation == null)
        {
            throw new InvalidOperationException("Rider, pickup, and dropoff locations are required to build a trip.");
        }
        return new Trip(this);
    }

    internal string Id => id;
    internal Rider Rider => rider;
    internal Location PickupLocation => pickupLocation;
    internal Location DropoffLocation => dropoffLocation;
    internal double Fare => fare;
}





enum DriverStatus
{
    ONLINE,
    IN_TRIP,
    OFFLINE
}




enum RideType
{
    SEDAN,
    SUV,
    AUTO
}





enum TripStatus
{
    REQUESTED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}









class Driver : User
{
    private Vehicle vehicle;
    private Location currentLocation;
    private DriverStatus status;

    public Driver(string name, string contact, Vehicle v, Location loc)
        : base(name, contact)
    {
        vehicle = v;
        currentLocation = loc;
        status = DriverStatus.OFFLINE;
    }

    public Vehicle Vehicle => vehicle;
    public DriverStatus Status => status;

    public void SetStatus(DriverStatus s)
    {
        status = s;
        Console.WriteLine($"Driver {Name} is now {s}");
    }

    public Location CurrentLocation => currentLocation;
    
    public void SetCurrentLocation(Location loc)
    {
        currentLocation = loc;
    }

    public override void OnUpdate(Trip trip)
    {
        Console.WriteLine($"--- Notification for Driver {Name} ---");
        Console.WriteLine($"  Trip {trip.Id} status: {trip.Status}.");
        if (trip.Status == TripStatus.REQUESTED)
        {
            Console.WriteLine("  A new ride is available for you to accept.");
        }
        Console.WriteLine("--------------------------------\n");
    }
}






class Location
{
    private readonly double latitude;
    private readonly double longitude;

    public Location(double lat, double lng)
    {
        latitude = lat;
        longitude = lng;
    }

    public double DistanceTo(Location other)
    {
        double dx = latitude - other.latitude;
        double dy = longitude - other.longitude;
        return Math.Sqrt(dx * dx + dy * dy);
    }

    public double Latitude => latitude;
    public double Longitude => longitude;

    public override string ToString()
    {
        return $"Location({latitude}, {longitude})";
    }
}





class Rider : User
{
    public Rider(string name, string contact) : base(name, contact) { }

    public override void OnUpdate(Trip trip)
    {
        Console.WriteLine($"--- Notification for Rider {Name} ---");
        Console.WriteLine($"  Trip {trip.Id} is now {trip.Status}.");
        if (trip.Driver != null)
        {
            Console.WriteLine($"  Driver: {trip.Driver.Name} in a {trip.Driver.Vehicle.Model} ({trip.Driver.Vehicle.LicenseNumber})");
        }
        Console.WriteLine("--------------------------------\n");
    }
}





// User abstract class
abstract class User : ITripObserver
{
    private readonly string id;
    private readonly string name;
    private readonly string contact;
    private readonly List<Trip> tripHistory;

    private static int idCounter = 0;

    public User(string n, string c)
    {
        id = $"user_{++idCounter}";
        name = n;
        contact = c;
        tripHistory = new List<Trip>();
    }

    public void AddTripToHistory(Trip trip)
    {
        tripHistory.Add(trip);
    }

    public List<Trip> TripHistory => tripHistory;
    public string Id => id;
    public string Name => name;
    public string Contact => contact;

    public abstract void OnUpdate(Trip trip);
}








class Vehicle
{
    private readonly string licenseNumber;
    private readonly string model;
    private readonly RideType type;

    public Vehicle(string license, string m, RideType t)
    {
        licenseNumber = license;
        model = m;
        type = t;
    }

    public string LicenseNumber => licenseNumber;
    public string Model => model;
    public RideType Type => type;
}








interface ITripObserver
{
    void OnUpdate(Trip trip);
}






class AssignedState : ITripState
{
    public void Request(Trip trip)
    {
        Console.WriteLine("Trip has already been requested and assigned.");
    }

    public void Assign(Trip trip, Driver driver)
    {
        Console.WriteLine("Trip is already assigned. To re-assign, cancel first.");
    }

    public void Start(Trip trip)
    {
        trip.SetStatus(TripStatus.IN_PROGRESS);
        trip.SetState(new InProgressState());
    }

    public void End(Trip trip)
    {
        Console.WriteLine("Cannot end a trip that has not started.");
    }
}





class CompletedState : ITripState
{
    public void Request(Trip trip)
    {
        Console.WriteLine("Cannot request a trip that is already completed.");
    }

    public void Assign(Trip trip, Driver driver)
    {
        Console.WriteLine("Cannot assign a driver to a completed trip.");
    }

    public void Start(Trip trip)
    {
        Console.WriteLine("Cannot start a completed trip.");
    }

    public void End(Trip trip)
    {
        Console.WriteLine("Trip is already completed.");
    }
}




class InProgressState : ITripState
{
    public void Request(Trip trip)
    {
        Console.WriteLine("Trip is already in progress.");
    }

    public void Assign(Trip trip, Driver driver)
    {
        Console.WriteLine("Cannot assign a new driver while trip is in progress.");
    }

    public void Start(Trip trip)
    {
        Console.WriteLine("Trip is already in progress.");
    }

    public void End(Trip trip)
    {
        trip.SetStatus(TripStatus.COMPLETED);
        trip.SetState(new CompletedState());
    }
}





interface ITripState
{
    void Request(Trip trip);
    void Assign(Trip trip, Driver driver);
    void Start(Trip trip);
    void End(Trip trip);
}









class RequestedState : ITripState
{
    public void Request(Trip trip)
    {
        Console.WriteLine("Trip is already in requested state.");
    }

    public void Assign(Trip trip, Driver driver)
    {
        trip.SetDriver(driver);
        trip.SetStatus(TripStatus.ASSIGNED);
        trip.SetState(new AssignedState());
    }

    public void Start(Trip trip)
    {
        Console.WriteLine("Cannot start a trip that has not been assigned a driver.");
    }

    public void End(Trip trip)
    {
        Console.WriteLine("Cannot end a trip that has not been assigned a driver.");
    }
}









interface IDriverMatchingStrategy
{
    List<Driver> FindDrivers(List<Driver> allDrivers, Location pickupLocation, RideType rideType);
}



class NearestDriverMatchingStrategy : IDriverMatchingStrategy
{
    private const double MAX_DISTANCE_KM = 5.0;

    public List<Driver> FindDrivers(List<Driver> allDrivers, Location pickupLocation, RideType rideType)
    {
        Console.WriteLine($"Finding nearest drivers for ride type: {rideType}");

        return allDrivers
            .Where(driver => driver.Status == DriverStatus.ONLINE)
            .Where(driver => driver.Vehicle.Type == rideType)
            .Where(driver => pickupLocation.DistanceTo(driver.CurrentLocation) <= MAX_DISTANCE_KM)
            .OrderBy(driver => pickupLocation.DistanceTo(driver.CurrentLocation))
            .ToList();
    }
}






class FlatRatePricingStrategy : IPricingStrategy
{
    private const double BASE_FARE = 5.0;
    private const double FLAT_RATE = 1.5;

    public double CalculateFare(Location pickup, Location dropoff, RideType rideType)
    {
        double distance = pickup.DistanceTo(dropoff);
        return BASE_FARE + distance * FLAT_RATE;
    }
}





interface IPricingStrategy
{
    double CalculateFare(Location pickup, Location dropoff, RideType rideType);
}




class VehicleBasedPricingStrategy : IPricingStrategy
{
    private const double BASE_FARE = 2.50;
    private readonly Dictionary<RideType, double> ratePerKm = new Dictionary<RideType, double>
    {
        { RideType.SEDAN, 1.50 },
        { RideType.SUV, 2.00 },
        { RideType.AUTO, 1.00 }
    };

    public double CalculateFare(Location pickup, Location dropoff, RideType rideType)
    {
        return BASE_FARE + ratePerKm[rideType] * pickup.DistanceTo(dropoff);
    }
}





class RideSharingService
{
    private static volatile RideSharingService instance;
    private static readonly object lockObject = new object();
    
    private readonly Dictionary<string, Rider> riders = new Dictionary<string, Rider>();
    private readonly Dictionary<string, Driver> drivers = new Dictionary<string, Driver>();
    private readonly Dictionary<string, Trip> trips = new Dictionary<string, Trip>();
    private IPricingStrategy pricingStrategy;
    private IDriverMatchingStrategy driverMatchingStrategy;

    private RideSharingService() { }

    public static RideSharingService Instance
    {
        get
        {
            if (instance == null)
            {
                lock (lockObject)
                {
                    if (instance == null)
                    {
                        instance = new RideSharingService();
                    }
                }
            }
            return instance;
        }
    }

    public void SetPricingStrategy(IPricingStrategy strategy)
    {
        pricingStrategy = strategy;
    }

    public void SetDriverMatchingStrategy(IDriverMatchingStrategy strategy)
    {
        driverMatchingStrategy = strategy;
    }

    public Rider RegisterRider(string name, string contact)
    {
        var rider = new Rider(name, contact);
        riders[rider.Id] = rider;
        return rider;
    }

    public Driver RegisterDriver(string name, string contact, Vehicle vehicle, Location initialLocation)
    {
        var driver = new Driver(name, contact, vehicle, initialLocation);
        drivers[driver.Id] = driver;
        return driver;
    }

    public Trip RequestRide(string riderId, Location pickup, Location dropoff, RideType rideType)
    {
        if (!riders.TryGetValue(riderId, out Rider rider))
        {
            throw new ArgumentException("Rider not found");
        }

        Console.WriteLine($"\n--- New Ride Request from {rider.Name} ---");

        // 1. Find available drivers
        var availableDrivers = driverMatchingStrategy.FindDrivers(drivers.Values.ToList(), pickup, rideType);

        if (!availableDrivers.Any())
        {
            Console.WriteLine("No drivers available for your request. Please try again later.");
            return null;
        }

        Console.WriteLine($"Found {availableDrivers.Count} available driver(s).");

        // 2. Calculate fare
        double fare = pricingStrategy.CalculateFare(pickup, dropoff, rideType);
        Console.WriteLine($"Estimated fare: ${fare:F2}");

        // 3. Create a trip using the Builder
        var trip = new TripBuilder()
            .WithRider(rider)
            .WithPickupLocation(pickup)
            .WithDropoffLocation(dropoff)
            .WithFare(fare)
            .Build();

        trips[trip.Id] = trip;

        // 4. Notify nearby drivers
        Console.WriteLine("Notifying nearby drivers of the new ride request...");
        foreach (var driver in availableDrivers)
        {
            Console.WriteLine($" > Notifying {driver.Name} at {driver.CurrentLocation}");
            driver.OnUpdate(trip);
        }

        return trip;
    }

    public void AcceptRide(string driverId, string tripId)
    {
        if (!drivers.TryGetValue(driverId, out Driver driver) || !trips.TryGetValue(tripId, out Trip trip))
        {
            throw new ArgumentException("Driver or Trip not found");
        }

        Console.WriteLine($"\n--- Driver {driver.Name} accepted the ride ---");

        driver.SetStatus(DriverStatus.IN_TRIP);
        trip.AssignDriver(driver);
    }

    public void StartTrip(string tripId)
    {
        if (!trips.TryGetValue(tripId, out Trip trip))
        {
            throw new ArgumentException("Trip not found");
        }
        Console.WriteLine($"\n--- Trip {trip.Id} is starting ---");
        trip.StartTrip();
    }

    public void EndTrip(string tripId)
    {
        if (!trips.TryGetValue(tripId, out Trip trip))
        {
            throw new ArgumentException("Trip not found");
        }
        Console.WriteLine($"\n--- Trip {trip.Id} is ending ---");
        trip.EndTrip();

        // Update statuses and history
        var driver = trip.Driver;
        driver.SetStatus(DriverStatus.ONLINE);
        driver.SetCurrentLocation(trip.DropoffLocation);

        var rider = trip.Rider;
        driver.AddTripToHistory(trip);
        rider.AddTripToHistory(trip);

        Console.WriteLine($"Driver {driver.Name} is now back online at {driver.CurrentLocation}");
    }
}







using System;
using System.Collections.Generic;
using System.Linq;

public class RideSharingServiceDemo
{
    public static void Main()
    {
        // 1. Setup the system using singleton instance
        var service = RideSharingService.Instance;
        service.SetDriverMatchingStrategy(new NearestDriverMatchingStrategy());
        service.SetPricingStrategy(new VehicleBasedPricingStrategy());

        // 2. Register riders and drivers
        var alice = service.RegisterRider("Alice", "123-456-7890");

        var bobVehicle = new Vehicle("KA01-1234", "Toyota Prius", RideType.SEDAN);
        var bob = service.RegisterDriver("Bob", "243-987-2860", bobVehicle, new Location(1.0, 1.0));

        var charlieVehicle = new Vehicle("KA02-5678", "Honda CRV", RideType.SUV);
        var charlie = service.RegisterDriver("Charlie", "313-486-2691", charlieVehicle, new Location(2.0, 2.0));

        var davidVehicle = new Vehicle("KA03-9012", "Honda CRV", RideType.SEDAN);
        var david = service.RegisterDriver("David", "613-586-3241", davidVehicle, new Location(1.2, 1.2));

        // 3. Drivers go online
        bob.SetStatus(DriverStatus.ONLINE);
        charlie.SetStatus(DriverStatus.ONLINE);
        david.SetStatus(DriverStatus.ONLINE);

        // David is online but will be too far for the first request
        david.SetCurrentLocation(new Location(10.0, 10.0));

        // 4. Alice requests a ride
        var pickupLocation = new Location(0.0, 0.0);
        var dropoffLocation = new Location(5.0, 5.0);

        // Rider wants a SEDAN
        var trip1 = service.RequestRide(alice.Id, pickupLocation, dropoffLocation, RideType.SEDAN);

        if (trip1 != null)
        {
            // 5. One of the nearby drivers accepts the ride
            service.AcceptRide(bob.Id, trip1.Id);

            // 6. The trip progresses
            service.StartTrip(trip1.Id);
            service.EndTrip(trip1.Id);
        }

        Console.WriteLine("\n--- Checking Trip History ---");
        Console.WriteLine($"Alice's trip history: {alice.TripHistory.Count} trips");
        Console.WriteLine($"Bob's trip history: {bob.TripHistory.Count} trips");

        // --- Second ride request ---
        Console.WriteLine("\n=============================================");
        var harry = service.RegisterRider("Harry", "167-342-7834");

        // Harry requests an SUV
        var trip2 = service.RequestRide(harry.Id,
            new Location(2.5, 2.5),
            new Location(8.0, 8.0),
            RideType.SUV);

        if (trip2 != null)
        {
            // Only Charlie is available for an SUV ride
            service.AcceptRide(charlie.Id, trip2.Id);
            service.StartTrip(trip2.Id);
            service.EndTrip(trip2.Id);
        }
    }
}





















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































