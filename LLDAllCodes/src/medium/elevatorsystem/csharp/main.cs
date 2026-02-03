

enum Direction
{
    UP,
    DOWN,
    IDLE
}


enum RequestSource
{
    INTERNAL, // From inside the cabin
    EXTERNAL  // From the hall/floor
}










class Elevator
{
    private readonly int id;
    private int currentFloor;
    private IElevatorState state;
    private volatile bool isRunning = true;

    private readonly SortedSet<int> upRequests;
    private readonly SortedSet<int> downRequests;
    private readonly object lockObject = new object();

    // Observer Pattern: List of observers
    private readonly List<IElevatorObserver> observers = new List<IElevatorObserver>();

    public Elevator(int id)
    {
        this.id = id;
        this.currentFloor = 1;
        this.upRequests = new SortedSet<int>();
        this.downRequests = new SortedSet<int>(Comparer<int>.Create((a, b) => b.CompareTo(a)));
        this.state = new IdleState();
    }

    // --- Observer Pattern Methods ---
    public void AddObserver(IElevatorObserver observer)
    {
        observers.Add(observer);
        observer.Update(this); // Send initial state
    }

    public void NotifyObservers()
    {
        foreach (IElevatorObserver observer in observers)
        {
            observer.Update(this);
        }
    }

    // --- State Pattern Methods ---
    public void SetState(IElevatorState state)
    {
        this.state = state;
        NotifyObservers(); // Notify observers on direction change
    }

    public void Move()
    {
        state.Move(this);
    }

    // --- Request Handling ---
    public void AddRequest(Request request)
    {
        lock (lockObject)
        {
            Console.WriteLine($"Elevator {id} processing: {request}");
            state.AddRequest(this, request);
        }
    }

    // --- Getters and Setters ---
    public int GetId() => id;
    
    public int GetCurrentFloor()
    {
        lock (lockObject)
        {
            return currentFloor;
        }
    }

    public void SetCurrentFloor(int floor)
    {
        lock (lockObject)
        {
            this.currentFloor = floor;
        }
        NotifyObservers(); // Notify observers on floor change
    }

    public Direction GetDirection() => state.GetDirection();
    public SortedSet<int> GetUpRequests() => upRequests;
    public SortedSet<int> GetDownRequests() => downRequests;
    public bool IsRunning() => isRunning;
    public void StopElevator() => isRunning = false;

    public void Run()
    {
        while (isRunning)
        {
            Move();
            try
            {
                Thread.Sleep(1000); // Simulate movement time
            }
            catch (ThreadInterruptedException)
            {
                isRunning = false;
            }
        }
    }
}






class Request
{
    private readonly int targetFloor;
    private readonly Direction direction;
    private readonly RequestSource source;

    public Request(int targetFloor, Direction direction, RequestSource source)
    {
        this.targetFloor = targetFloor;
        this.direction = direction;
        this.source = source;
    }

    public int GetTargetFloor() => targetFloor;
    public Direction GetDirection() => direction;
    public RequestSource GetSource() => source;

    public override string ToString()
    {
        if (source == RequestSource.EXTERNAL)
        {
            return $"{source} Request to floor {targetFloor} going {direction}";
        }
        else
        {
            return $"{source} Request to floor {targetFloor}";
        }
    }
}






class Display : IElevatorObserver
{
    public void Update(Elevator elevator)
    {
        Console.WriteLine($"[DISPLAY] Elevator {elevator.GetId()} | Current Floor: {elevator.GetCurrentFloor()} | Direction: {elevator.GetDirection()}");
    }
}




interface IElevatorObserver
{
    void Update(Elevator elevator);
}







class IdleState : IElevatorState
{
    public void Move(Elevator elevator)
    {
        if (elevator.GetUpRequests().Count > 0)
        {
            elevator.SetState(new MovingUpState());
        }
        else if (elevator.GetDownRequests().Count > 0)
        {
            elevator.SetState(new MovingDownState());
        }
        // Else stay idle
    }

    public void AddRequest(Elevator elevator, Request request)
    {
        if (request.GetTargetFloor() > elevator.GetCurrentFloor())
        {
            elevator.GetUpRequests().Add(request.GetTargetFloor());
        }
        else if (request.GetTargetFloor() < elevator.GetCurrentFloor())
        {
            elevator.GetDownRequests().Add(request.GetTargetFloor());
        }
        // If request is for current floor, doors would open (handled implicitly by moving to that floor)
    }

    public Direction GetDirection() => Direction.IDLE;
}





interface IElevatorState
{
    void Move(Elevator elevator);
    void AddRequest(Elevator elevator, Request request);
    Direction GetDirection();
}




class MovingDownState : IElevatorState
{
    public void Move(Elevator elevator)
    {
        if (elevator.GetDownRequests().Count == 0)
        {
            elevator.SetState(new IdleState());
            return;
        }

        int nextFloor = elevator.GetDownRequests().Max();
        elevator.SetCurrentFloor(elevator.GetCurrentFloor() - 1);

        if (elevator.GetCurrentFloor() == nextFloor)
        {
            Console.WriteLine($"Elevator {elevator.GetId()} stopped at floor {nextFloor}");
            elevator.GetDownRequests().Remove(nextFloor);
        }

        if (elevator.GetDownRequests().Count == 0)
        {
            elevator.SetState(new IdleState());
        }
    }

    public void AddRequest(Elevator elevator, Request request)
    {
        // Internal requests always get added to the appropriate queue
        if (request.GetSource() == RequestSource.INTERNAL)
        {
            if (request.GetTargetFloor() > elevator.GetCurrentFloor())
            {
                elevator.GetUpRequests().Add(request.GetTargetFloor());
            }
            else
            {
                elevator.GetDownRequests().Add(request.GetTargetFloor());
            }
            return;
        }

        // External requests
        if (request.GetDirection() == Direction.DOWN && request.GetTargetFloor() <= elevator.GetCurrentFloor())
        {
            elevator.GetDownRequests().Add(request.GetTargetFloor());
        }
        else if (request.GetDirection() == Direction.UP)
        {
            elevator.GetUpRequests().Add(request.GetTargetFloor());
        }
    }

    public Direction GetDirection() => Direction.DOWN;
}





class MovingUpState : IElevatorState
{
    public void Move(Elevator elevator)
    {
        if (elevator.GetUpRequests().Count == 0)
        {
            elevator.SetState(new IdleState());
            return;
        }

        int nextFloor = elevator.GetUpRequests().Min();
        elevator.SetCurrentFloor(elevator.GetCurrentFloor() + 1);

        if (elevator.GetCurrentFloor() == nextFloor)
        {
            Console.WriteLine($"Elevator {elevator.GetId()} stopped at floor {nextFloor}");
            elevator.GetUpRequests().Remove(nextFloor);
        }

        if (elevator.GetUpRequests().Count == 0)
        {
            elevator.SetState(new IdleState());
        }
    }

    public void AddRequest(Elevator elevator, Request request)
    {
        // Internal requests always get added to the appropriate queue
        if (request.GetSource() == RequestSource.INTERNAL)
        {
            if (request.GetTargetFloor() > elevator.GetCurrentFloor())
            {
                elevator.GetUpRequests().Add(request.GetTargetFloor());
            }
            else
            {
                elevator.GetDownRequests().Add(request.GetTargetFloor());
            }
            return;
        }

        // External requests
        if (request.GetDirection() == Direction.UP && request.GetTargetFloor() >= elevator.GetCurrentFloor())
        {
            elevator.GetUpRequests().Add(request.GetTargetFloor());
        }
        else if (request.GetDirection() == Direction.DOWN)
        {
            elevator.GetDownRequests().Add(request.GetTargetFloor());
        }
    }

    public Direction GetDirection() => Direction.UP;
}







interface IElevatorSelectionStrategy
{
    Elevator SelectElevator(List<Elevator> elevators, Request request);
}




class NearestElevatorStrategy : IElevatorSelectionStrategy
{
    public Elevator SelectElevator(List<Elevator> elevators, Request request)
    {
        Elevator bestElevator = null;
        int minDistance = int.MaxValue;

        foreach (Elevator elevator in elevators)
        {
            if (IsSuitable(elevator, request))
            {
                int distance = Math.Abs(elevator.GetCurrentFloor() - request.GetTargetFloor());
                if (distance < minDistance)
                {
                    minDistance = distance;
                    bestElevator = elevator;
                }
            }
        }
        return bestElevator;
    }

    private bool IsSuitable(Elevator elevator, Request request)
    {
        if (elevator.GetDirection() == Direction.IDLE)
            return true;
        if (elevator.GetDirection() == request.GetDirection())
        {
            if (request.GetDirection() == Direction.UP && elevator.GetCurrentFloor() <= request.GetTargetFloor())
                return true;
            if (request.GetDirection() == Direction.DOWN && elevator.GetCurrentFloor() >= request.GetTargetFloor())
                return true;
        }
        return false;
    }
}










class ElevatorSystem
{
    private static ElevatorSystem instance;
    private static readonly object lockObject = new object();

    private readonly Dictionary<int, Elevator> elevators;
    private readonly IElevatorSelectionStrategy selectionStrategy;
    private readonly List<Task> elevatorTasks;

    private ElevatorSystem(int numElevators)
    {
        this.selectionStrategy = new NearestElevatorStrategy();
        this.elevatorTasks = new List<Task>();

        List<Elevator> elevatorList = new List<Elevator>();
        Display display = new Display(); // Create the observer

        for (int i = 1; i <= numElevators; i++)
        {
            Elevator elevator = new Elevator(i);
            elevator.AddObserver(display); // Attach the observer
            elevatorList.Add(elevator);
        }

        this.elevators = elevatorList.ToDictionary(e => e.GetId(), e => e);
    }

    public static ElevatorSystem GetInstance(int numElevators)
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new ElevatorSystem(numElevators);
                }
            }
        }
        return instance;
    }

    public void Start()
    {
        foreach (Elevator elevator in elevators.Values)
        {
            elevatorTasks.Add(Task.Run(() => elevator.Run()));
        }
    }

    // --- Facade Methods ---

    // EXTERNAL Request (Hall Call)
    public void RequestElevator(int floor, Direction direction)
    {
        Console.WriteLine($"\n>> EXTERNAL Request: User at floor {floor} wants to go {direction}");
        Request request = new Request(floor, direction, RequestSource.EXTERNAL);

        // Use strategy to find the best elevator
        Elevator selectedElevator = selectionStrategy.SelectElevator(elevators.Values.ToList(), request);

        if (selectedElevator != null)
        {
            selectedElevator.AddRequest(request);
        }
        else
        {
            Console.WriteLine("System busy, please wait.");
        }
    }

    // INTERNAL Request (Cabin Call)
    public void SelectFloor(int elevatorId, int destinationFloor)
    {
        Console.WriteLine($"\n>> INTERNAL Request: User in Elevator {elevatorId} selected floor {destinationFloor}");
        Request request = new Request(destinationFloor, Direction.IDLE, RequestSource.INTERNAL);

        if (elevators.TryGetValue(elevatorId, out Elevator elevator))
        {
            elevator.AddRequest(request);
        }
        else
        {
            Console.Error.WriteLine("Invalid elevator ID.");
        }
    }

    public void Shutdown()
    {
        Console.WriteLine("Shutting down elevator system...");
        foreach (Elevator elevator in elevators.Values)
        {
            elevator.StopElevator();
        }
        Task.WaitAll(elevatorTasks.ToArray());
    }
}









using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using System.Linq;

public class ElevatorSystemDemo
{
    public static void Main(string[] args)
    {
        // Setup: A building with 2 elevators
        int numElevators = 2;
        // The GetInstance method now initializes the elevators and attaches the Display (Observer).
        ElevatorSystem elevatorSystem = ElevatorSystem.GetInstance(numElevators);

        // Start the elevator system
        elevatorSystem.Start();
        Console.WriteLine("Elevator system started. ConsoleDisplay is observing.\n");

        // --- SIMULATION START ---

        // 1. External Request: User at floor 5 wants to go UP.
        // The system will dispatch this to the nearest elevator (likely E1 or E2, both at floor 1).
        elevatorSystem.RequestElevator(5, Direction.UP);
        Thread.Sleep(100); // Wait for the elevator to start moving

        // 2. Internal Request: Assume E1 took the previous request.
        // The user gets in at floor 5 and presses 10.
        // We send this request directly to E1.

        // Note: In a real simulation, we'd wait until E1 reaches floor 5, but for this demo,
        // we simulate the internal button press shortly after the external one.
        elevatorSystem.SelectFloor(1, 10);
        Thread.Sleep(200);

        // 3. External Request: User at floor 3 wants to go DOWN.
        // E2 (likely still idle at floor 1) might take this, or E1 if it's convenient.
        elevatorSystem.RequestElevator(3, Direction.DOWN);
        Thread.Sleep(300);

        // 4. Internal Request: User in E2 presses 1.
        elevatorSystem.SelectFloor(2, 1);

        // Let the simulation run for a while to observe the display updates
        Console.WriteLine("\n--- Letting simulation run for 1 second ---");
        Thread.Sleep(1000);

        // Shutdown the system
        elevatorSystem.Shutdown();
        Console.WriteLine("\n--- SIMULATION END ---");
    }
}






































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































