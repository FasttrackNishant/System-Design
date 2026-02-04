


class ParkingFloor
{
    private readonly int floorNumber;
    private readonly Dictionary<string, ParkingSpot> spots;
    private readonly object lockObject = new object();

    public ParkingFloor(int floorNumber)
    {
        this.floorNumber = floorNumber;
        this.spots = new Dictionary<string, ParkingSpot>();
    }

    public void AddSpot(ParkingSpot spot)
    {
        spots[spot.GetSpotId()] = spot;
    }

    public ParkingSpot FindAvailableSpot(Vehicle vehicle)
    {
        lock (lockObject)
        {
            var availableSpots = spots.Values
                .Where(spot => !spot.IsOccupiedSpot() && spot.CanFitVehicle(vehicle))
                .OrderBy(spot => (int)spot.GetSpotSize())
                .ToList();

            return availableSpots.FirstOrDefault();
        }
    }

    public void DisplayAvailability()
    {
        Console.WriteLine($"--- Floor {floorNumber} Availability ---");
        var availableCounts = new Dictionary<VehicleSize, int>
        {
            { VehicleSize.SMALL, 0 },
            { VehicleSize.MEDIUM, 0 },
            { VehicleSize.LARGE, 0 }
        };

        foreach (var spot in spots.Values)
        {
            if (!spot.IsOccupiedSpot())
            {
                availableCounts[spot.GetSpotSize()]++;
            }
        }

        foreach (VehicleSize size in Enum.GetValues(typeof(VehicleSize)))
        {
            Console.WriteLine($"  {size} spots: {availableCounts[size]}");
        }
    }
}


















class ParkingSpot
{
    private readonly string spotId;
    private readonly VehicleSize spotSize;
    private bool isOccupied;
    private Vehicle parkedVehicle;
    private readonly object lockObject = new object();

    public ParkingSpot(string spotId, VehicleSize spotSize)
    {
        this.spotId = spotId;
        this.spotSize = spotSize;
        this.isOccupied = false;
        this.parkedVehicle = null;
    }

    public string GetSpotId()
    {
        return spotId;
    }

    public VehicleSize GetSpotSize()
    {
        return spotSize;
    }

    public bool IsAvailable()
    {
        lock (lockObject)
        {
            return !isOccupied;
        }
    }

    public bool IsOccupiedSpot()
    {
        return isOccupied;
    }

    public void ParkVehicle(Vehicle vehicle)
    {
        lock (lockObject)
        {
            this.parkedVehicle = vehicle;
            this.isOccupied = true;
        }
    }

    public void UnparkVehicle()
    {
        lock (lockObject)
        {
            this.parkedVehicle = null;
            this.isOccupied = false;
        }
    }

    public bool CanFitVehicle(Vehicle vehicle)
    {
        if (isOccupied) return false;

        switch (vehicle.GetSize())
        {
            case VehicleSize.SMALL:
                return spotSize == VehicleSize.SMALL;
            case VehicleSize.MEDIUM:
                return spotSize == VehicleSize.MEDIUM || spotSize == VehicleSize.LARGE;
            case VehicleSize.LARGE:
                return spotSize == VehicleSize.LARGE;
            default:
                return false;
        }
    }
}











class ParkingTicket
{
    private readonly string ticketId;
    private readonly Vehicle vehicle;
    private readonly ParkingSpot spot;
    private readonly long entryTimestamp;
    private long exitTimestamp;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot)
    {
        this.ticketId = Guid.NewGuid().ToString();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTimestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        this.exitTimestamp = 0;
    }

    public string GetTicketId() { return ticketId; }
    public Vehicle GetVehicle() { return vehicle; }
    public ParkingSpot GetSpot() { return spot; }
    public long GetEntryTimestamp() { return entryTimestamp; }
    public long GetExitTimestamp() { return exitTimestamp; }

    public void SetExitTimestamp()
    {
        this.exitTimestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }
}








abstract class Vehicle
{
    protected readonly string licenseNumber;
    protected readonly VehicleSize size;

    public Vehicle(string licenseNumber, VehicleSize size)
    {
        this.licenseNumber = licenseNumber;
        this.size = size;
    }

    public string GetLicenseNumber()
    {
        return licenseNumber;
    }

    public VehicleSize GetSize()
    {
        return size;
    }
}

class Bike : Vehicle
{
    public Bike(string licenseNumber) : base(licenseNumber, VehicleSize.SMALL)
    {
    }
}

class Car : Vehicle
{
    public Car(string licenseNumber) : base(licenseNumber, VehicleSize.MEDIUM)
    {
    }
}

class Truck : Vehicle
{
    public Truck(string licenseNumber) : base(licenseNumber, VehicleSize.LARGE)
    {
    }
}







enum VehicleSize
{
    SMALL,
    MEDIUM,
    LARGE
}










interface IFeeStrategy
{
    double CalculateFee(ParkingTicket parkingTicket);
}

class FlatRateFeeStrategy : IFeeStrategy
{
    private const double RATE_PER_HOUR = 10.0;

    public double CalculateFee(ParkingTicket parkingTicket)
    {
        long duration = parkingTicket.GetExitTimestamp() - parkingTicket.GetEntryTimestamp();
        long hours = (duration / (1000 * 60 * 60)) + 1;
        return hours * RATE_PER_HOUR;
    }
}

class VehicleBasedFeeStrategy : IFeeStrategy
{
    private static readonly Dictionary<VehicleSize, double> HOURLY_RATES = new Dictionary<VehicleSize, double>
    {
        { VehicleSize.SMALL, 10.0 },
        { VehicleSize.MEDIUM, 20.0 },
        { VehicleSize.LARGE, 30.0 }
    };

    public double CalculateFee(ParkingTicket parkingTicket)
    {
        long duration = parkingTicket.GetExitTimestamp() - parkingTicket.GetEntryTimestamp();
        long hours = (duration / (1000 * 60 * 60)) + 1;
        return hours * HOURLY_RATES[parkingTicket.GetVehicle().GetSize()];
    }
}












interface IParkingStrategy
{
    ParkingSpot FindSpot(List<ParkingFloor> floors, Vehicle vehicle);
}

class NearestFirstStrategy : IParkingStrategy
{
    public ParkingSpot FindSpot(List<ParkingFloor> floors, Vehicle vehicle)
    {
        foreach (var floor in floors)
        {
            var spot = floor.FindAvailableSpot(vehicle);
            if (spot != null)
            {
                return spot;
            }
        }
        return null;
    }
}

class FarthestFirstStrategy : IParkingStrategy
{
    public ParkingSpot FindSpot(List<ParkingFloor> floors, Vehicle vehicle)
    {
        var reversedFloors = floors.AsEnumerable().Reverse().ToList();
        foreach (var floor in reversedFloors)
        {
            var spot = floor.FindAvailableSpot(vehicle);
            if (spot != null)
            {
                return spot;
            }
        }
        return null;
    }
}

class BestFitStrategy : IParkingStrategy
{
    public ParkingSpot FindSpot(List<ParkingFloor> floors, Vehicle vehicle)
    {
        ParkingSpot bestSpot = null;

        foreach (var floor in floors)
        {
            var spotOnThisFloor = floor.FindAvailableSpot(vehicle);

            if (spotOnThisFloor != null)
            {
                if (bestSpot == null)
                {
                    bestSpot = spotOnThisFloor;
                }
                else
                {
                    if ((int)spotOnThisFloor.GetSpotSize() < (int)bestSpot.GetSpotSize())
                    {
                        bestSpot = spotOnThisFloor;
                    }
                }
            }
        }
        return bestSpot;
    }
}








class ParkingLot
{
    private static ParkingLot instance;
    private static readonly object instanceLock = new object();
    private readonly List<ParkingFloor> floors;
    private readonly ConcurrentDictionary<string, ParkingTicket> activeTickets;
    private IFeeStrategy feeStrategy;
    private IParkingStrategy parkingStrategy;
    private readonly object mainLock = new object();

    private ParkingLot()
    {
        floors = new List<ParkingFloor>();
        activeTickets = new ConcurrentDictionary<string, ParkingTicket>();
        feeStrategy = new FlatRateFeeStrategy();
        parkingStrategy = new NearestFirstStrategy();
    }

    public static ParkingLot GetInstance()
    {
        if (instance == null)
        {
            lock (instanceLock)
            {
                if (instance == null)
                {
                    instance = new ParkingLot();
                }
            }
        }
        return instance;
    }

    public void AddFloor(ParkingFloor floor)
    {
        floors.Add(floor);
    }

    public void SetFeeStrategy(IFeeStrategy feeStrategy)
    {
        this.feeStrategy = feeStrategy;
    }

    public void SetParkingStrategy(IParkingStrategy parkingStrategy)
    {
        this.parkingStrategy = parkingStrategy;
    }

    public ParkingTicket ParkVehicle(Vehicle vehicle)
    {
        lock (mainLock)
        {
            var spot = parkingStrategy.FindSpot(floors, vehicle);
            if (spot != null)
            {
                spot.ParkVehicle(vehicle);
                var ticket = new ParkingTicket(vehicle, spot);
                activeTickets.TryAdd(vehicle.GetLicenseNumber(), ticket);
                Console.WriteLine($"Vehicle {vehicle.GetLicenseNumber()} parked at spot {spot.GetSpotId()}");
                return ticket;
            }
            else
            {
                Console.WriteLine($"No available spot for vehicle {vehicle.GetLicenseNumber()}");
                return null;
            }
        }
    }

    public double? UnparkVehicle(string licenseNumber)
    {
        lock (mainLock)
        {
            if (!activeTickets.TryRemove(licenseNumber, out ParkingTicket ticket))
            {
                Console.WriteLine($"Ticket not found for vehicle {licenseNumber}");
                return null;
            }

            ticket.GetSpot().UnparkVehicle();
            ticket.SetExitTimestamp();
            double fee = feeStrategy.CalculateFee(ticket);
            Console.WriteLine($"Vehicle {licenseNumber} unparked from spot {ticket.GetSpot().GetSpotId()}");
            return fee;
        }
    }
}














using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;

public class ParkingLotDemo
{
    public static void Main(string[] args)
    {
        var parkingLot = ParkingLot.GetInstance();

        // 1. Initialize the parking lot with floors and spots
        var floor1 = new ParkingFloor(1);
        floor1.AddSpot(new ParkingSpot("F1-S1", VehicleSize.SMALL));
        floor1.AddSpot(new ParkingSpot("F1-M1", VehicleSize.MEDIUM));
        floor1.AddSpot(new ParkingSpot("F1-L1", VehicleSize.LARGE));

        var floor2 = new ParkingFloor(2);
        floor2.AddSpot(new ParkingSpot("F2-M1", VehicleSize.MEDIUM));
        floor2.AddSpot(new ParkingSpot("F2-M2", VehicleSize.MEDIUM));

        parkingLot.AddFloor(floor1);
        parkingLot.AddFloor(floor2);

        parkingLot.SetFeeStrategy(new VehicleBasedFeeStrategy());

        // 2. Simulate vehicle entries
        Console.WriteLine("\n--- Vehicle Entries ---");
        floor1.DisplayAvailability();
        floor2.DisplayAvailability();

        var bike = new Bike("B-123");
        var car = new Car("C-456");
        var truck = new Truck("T-789");

        var bikeTicket = parkingLot.ParkVehicle(bike);
        var carTicket = parkingLot.ParkVehicle(car);
        var truckTicket = parkingLot.ParkVehicle(truck);

        Console.WriteLine("\n--- Availability after parking ---");
        floor1.DisplayAvailability();
        floor2.DisplayAvailability();

        // 3. Simulate another car entry (should go to floor 2)
        var car2 = new Car("C-999");
        var car2Ticket = parkingLot.ParkVehicle(car2);

        // 4. Simulate a vehicle entry that fails (no available spots)
        var bike2 = new Bike("B-000");
        var failedBikeTicket = parkingLot.ParkVehicle(bike2);

        // 5. Simulate vehicle exits and fee calculation
        Console.WriteLine("\n--- Vehicle Exits ---");

        if (carTicket != null)
        {
            var fee = parkingLot.UnparkVehicle(car.GetLicenseNumber());
            if (fee.HasValue)
            {
                Console.WriteLine($"Car C-456 unparked. Fee: ${fee.Value:F2}");
            }
        }

        Console.WriteLine("\n--- Availability after one car leaves ---");
        floor1.DisplayAvailability();
        floor2.DisplayAvailability();
    }
}

































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































