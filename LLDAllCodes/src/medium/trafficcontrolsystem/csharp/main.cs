
enum Direction
{
    NORTH,
    SOUTH,
    EAST,
    WEST
}


enum LightColor
{
    GREEN,
    YELLOW,
    RED
}



class CentralMonitor : ITrafficObserver
{
    public void Update(int intersectionId, Direction direction, LightColor color)
    {
        Console.WriteLine($"[MONITOR] Intersection {intersectionId}: Light for {direction} direction changed to {color}.");
    }
}



interface ITrafficObserver
{
    void Update(int intersectionId, Direction direction, LightColor color);
}




class EastWestGreenState : IIntersectionState
{
    public void Handle(IntersectionController context)
    {
        Console.WriteLine($"\n--- INTERSECTION {context.GetId()}: Cycle -> East-West GREEN ---");

        // Turn East and West green, ensure North and South are red
        context.GetLight(Direction.EAST).StartGreen();
        context.GetLight(Direction.WEST).StartGreen();
        context.GetLight(Direction.NORTH).SetColor(LightColor.RED);
        context.GetLight(Direction.SOUTH).SetColor(LightColor.RED);

        // Wait for green light duration
        Thread.Sleep((int)context.GetGreenDuration());

        // Transition East and West to Yellow
        context.GetLight(Direction.EAST).Transition();
        context.GetLight(Direction.WEST).Transition();

        // Wait for yellow light duration
        Thread.Sleep((int)context.GetYellowDuration());

        // Transition East and West to Red
        context.GetLight(Direction.EAST).Transition();
        context.GetLight(Direction.WEST).Transition();

        // Change the intersection's state back to let North-South go
        context.SetState(new NorthSouthGreenState());
    }
}



interface IIntersectionState
{
    void Handle(IntersectionController context);
}



class NorthSouthGreenState : IIntersectionState
{
    public void Handle(IntersectionController context)
    {
        Console.WriteLine($"\n--- INTERSECTION {context.GetId()}: Cycle Start -> North-South GREEN ---");

        // Turn North and South green, ensure East and West are red
        context.GetLight(Direction.NORTH).StartGreen();
        context.GetLight(Direction.SOUTH).StartGreen();
        context.GetLight(Direction.EAST).SetColor(LightColor.RED);
        context.GetLight(Direction.WEST).SetColor(LightColor.RED);

        // Wait for green light duration
        Thread.Sleep((int)context.GetGreenDuration());

        // Transition North and South to Yellow
        context.GetLight(Direction.NORTH).Transition();
        context.GetLight(Direction.SOUTH).Transition();

        // Wait for yellow light duration
        Thread.Sleep((int)context.GetYellowDuration());

        // Transition North and South to Red
        context.GetLight(Direction.NORTH).Transition();
        context.GetLight(Direction.SOUTH).Transition();

        // Change the intersection's state to let East-West go
        context.SetState(new EastWestGreenState());
    }
}









class GreenState : ISignalState
{
    public void Handle(TrafficLight context)
    {
        context.SetColor(LightColor.GREEN);
        // After being green, the next state is yellow.
        context.SetNextState(new YellowState());
    }
}



interface ISignalState
{
    void Handle(TrafficLight context);
}



class RedState : ISignalState
{
    public void Handle(TrafficLight context)
    {
        context.SetColor(LightColor.RED);
        // Red is a stable state, it transitions to green only when the intersection controller commands it.
        // So, the next state is self.
        context.SetNextState(new RedState());
    }
}





class YellowState : ISignalState
{
    public void Handle(TrafficLight context)
    {
        context.SetColor(LightColor.YELLOW);
        // After being yellow, the next state is red.
        context.SetNextState(new RedState());
    }
}











class IntersectionController
{
    private readonly int id;
    private readonly Dictionary<Direction, TrafficLight> trafficLights;
    private IIntersectionState currentState;
    private readonly long greenDuration;
    private readonly long yellowDuration;
    private volatile bool running = true;

    public IntersectionController(int id, Dictionary<Direction, TrafficLight> trafficLights, 
                                  long greenDuration, long yellowDuration)
    {
        this.id = id;
        this.trafficLights = trafficLights;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.currentState = new NorthSouthGreenState(); // Initial state for the intersection
    }

    public int GetId() => id;
    public long GetGreenDuration() => greenDuration;
    public long GetYellowDuration() => yellowDuration;
    public TrafficLight GetLight(Direction direction) => trafficLights[direction];

    public void SetState(IIntersectionState state)
    {
        this.currentState = state;
    }

    public void Start()
    {
        Task.Run(() => Run());
    }

    public void Stop()
    {
        this.running = false;
    }

    public void Run()
    {
        while (running)
        {
            try
            {
                currentState.Handle(this);
            }
            catch (Exception e)
            {
                Console.WriteLine($"Intersection {id} encountered an error: {e.Message}");
                running = false;
            }
        }
    }
}

// Builder Pattern
class IntersectionControllerBuilder
{
    private readonly int id;
    private long greenDuration = 5000; // default 5s
    private long yellowDuration = 2000; // default 2s
    private readonly List<ITrafficObserver> observers = new List<ITrafficObserver>();

    public IntersectionControllerBuilder(int id)
    {
        this.id = id;
    }

    public IntersectionControllerBuilder WithDurations(long green, long yellow)
    {
        this.greenDuration = green;
        this.yellowDuration = yellow;
        return this;
    }

    public IntersectionControllerBuilder AddObserver(ITrafficObserver observer)
    {
        this.observers.Add(observer);
        return this;
    }

    public IntersectionController Build()
    {
        var lights = new Dictionary<Direction, TrafficLight>();
        foreach (Direction dir in Enum.GetValues(typeof(Direction)).Cast<Direction>())
        {
            TrafficLight light = new TrafficLight(id, dir);
            // Attach all registered observers to each light
            foreach (var observer in observers)
            {
                light.AddObserver(observer);
            }
            lights[dir] = light;
        }
        return new IntersectionController(id, lights, greenDuration, yellowDuration);
    }
}









class TrafficControlSystem
{
    private static TrafficControlSystem instance;
    private static readonly object lockObject = new object();
    private readonly List<IntersectionController> intersections = new List<IntersectionController>();
    private readonly List<Task> tasks = new List<Task>();

    private TrafficControlSystem() { }

    public static TrafficControlSystem GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new TrafficControlSystem();
                }
            }
        }
        return instance;
    }

    public void AddIntersection(int intersectionId, int greenDuration, int yellowDuration)
    {
        IntersectionController intersection = new IntersectionControllerBuilder(intersectionId)
                .WithDurations(greenDuration, yellowDuration)
                .AddObserver(new CentralMonitor())
                .Build();
        intersections.Add(intersection);
    }

    public void StartSystem()
    {
        if (intersections.Count == 0)
        {
            Console.WriteLine("No intersections to manage. System not starting.");
            return;
        }

        Console.WriteLine("--- Starting Traffic Control System ---");

        foreach (var intersection in intersections)
        {
            Task task = Task.Run(() => intersection.Run());
            tasks.Add(task);
        }
    }

    public void StopSystem()
    {
        Console.WriteLine("\n--- Shutting Down Traffic Control System ---");

        foreach (var intersection in intersections)
        {
            intersection.Stop();
        }

        Task.WaitAll(tasks.ToArray(), TimeSpan.FromSeconds(5));

        Console.WriteLine("All intersections stopped. System shut down.");
    }
}







class TrafficLight
{
    private readonly int intersectionId;
    private readonly Direction direction;
    private LightColor currentColor;
    private ISignalState currentState;
    private ISignalState nextState;
    private readonly List<ITrafficObserver> observers = new List<ITrafficObserver>();

    public TrafficLight(int intersectionId, Direction direction)
    {
        this.intersectionId = intersectionId;
        this.direction = direction;
        this.currentState = new RedState(); // Default state is Red
        this.currentState.Handle(this);
    }

    // This is called by the IntersectionController to initiate a G-Y-R cycle
    public void StartGreen()
    {
        this.currentState = new GreenState();
        this.currentState.Handle(this);
    }

    // This is called by the IntersectionController to transition from G->Y or Y->R
    public void Transition()
    {
        this.currentState = this.nextState;
        this.currentState.Handle(this);
    }

    public void SetColor(LightColor color)
    {
        if (this.currentColor != color)
        {
            this.currentColor = color;
            NotifyObservers();
        }
    }

    public void SetNextState(ISignalState state)
    {
        this.nextState = state;
    }

    public LightColor GetCurrentColor() => currentColor;
    public Direction GetDirection() => direction;

    // Observer pattern methods
    public void AddObserver(ITrafficObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(ITrafficObserver observer)
    {
        observers.Remove(observer);
    }

    private void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.Update(intersectionId, direction, currentColor);
        }
    }
}







using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;

public class TrafficSystemDemo
{
    public static void Main(string[] args)
    {
        // 1. Get the singleton TrafficControlSystem instance
        TrafficControlSystem system = TrafficControlSystem.GetInstance();

        // 2. Add intersections to the system
        system.AddIntersection(1, 500, 200);
        system.AddIntersection(2, 700, 150);

        // 3. Start the system
        system.StartSystem();

        // 4. Let the simulation run for a while (e.g., 5 seconds)
        try
        {
            Thread.Sleep(5000);
        }
        catch (ThreadInterruptedException)
        {
            Thread.CurrentThread.Interrupt();
        }

        // 5. Stop the system gracefully
        system.StopSystem();
    }
}













































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































