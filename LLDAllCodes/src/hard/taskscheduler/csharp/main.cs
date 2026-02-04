class LoggingObserver : TaskExecutionObserver
{
    public override void OnTaskStarted(ScheduledTask task)
    {
        Console.WriteLine($"[LOG - {DateTime.Now:HH:mm:ss.fff}] [{Thread.CurrentThread.Name ?? Thread.CurrentThread.ManagedThreadId.ToString()}] Task {task.GetId()} started.");
    }

    public override void OnTaskCompleted(ScheduledTask task)
    {
        Console.WriteLine($"[LOG - {DateTime.Now:HH:mm:ss.fff}] [{Thread.CurrentThread.Name ?? Thread.CurrentThread.ManagedThreadId.ToString()}] Task {task.GetId()} completed successfully.");
    }

    public override void OnTaskFailed(ScheduledTask task, Exception exception)
    {
        Console.WriteLine($"[LOG - {DateTime.Now:HH:mm:ss.fff}] [{Thread.CurrentThread.Name ?? Thread.CurrentThread.ManagedThreadId.ToString()}] Task {task.GetId()} failed: {exception.Message}");
    }
}






abstract class TaskExecutionObserver
{
    public abstract void OnTaskStarted(ScheduledTask task);
    public abstract void OnTaskCompleted(ScheduledTask task);
    public abstract void OnTaskFailed(ScheduledTask task, Exception exception);
}




class OneTimeSchedulingStrategy : SchedulingStrategy
{
    private readonly DateTime executionTime;

    public OneTimeSchedulingStrategy(DateTime executionTime)
    {
        this.executionTime = executionTime;
    }

    public override DateTime? GetNextExecutionTime(DateTime? lastExecutionTime)
    {
        return lastExecutionTime == null ? executionTime : null;
    }
}




class RecurringSchedulingStrategy : SchedulingStrategy
{
    private readonly TimeSpan interval;

    public RecurringSchedulingStrategy(TimeSpan interval)
    {
        this.interval = interval;
    }

    public override DateTime? GetNextExecutionTime(DateTime? lastExecutionTime)
    {
        DateTime baseTime = lastExecutionTime ?? DateTime.Now;
        return baseTime.Add(interval);
    }
}




abstract class SchedulingStrategy
{
    public abstract DateTime? GetNextExecutionTime(DateTime? lastExecutionTime);
}














class DataBackupTask : TaskBase
{
    private readonly string source;
    private readonly string destination;

    public DataBackupTask(string source, string destination)
    {
        this.source = source;
        this.destination = destination;
    }

    public override void Execute()
    {
        Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Executing DataBackupTask: Backing up from {source} to {destination}...");
        Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] DataBackupTask: Backup complete.");
    }
}







class PrintMessageTask : TaskBase
{
    private readonly string message;

    public PrintMessageTask(string message)
    {
        this.message = message;
    }

    public override void Execute()
    {
        Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Executing PrintMessageTask: {message}");
    }
}






abstract class TaskBase
{
    public abstract void Execute();
}






class ScheduledTask : IComparable<ScheduledTask>
{
    private readonly string id;
    private readonly TaskBase task;
    private readonly SchedulingStrategy strategy;
    private DateTime? nextExecutionTime;
    private DateTime? lastExecutionTime;

    public ScheduledTask(TaskBase task, SchedulingStrategy strategy)
    {
        this.id = Guid.NewGuid().ToString();
        this.task = task;
        this.strategy = strategy;
        UpdateNextExecutionTime();
    }

    public void UpdateNextExecutionTime()
    {
        nextExecutionTime = strategy.GetNextExecutionTime(lastExecutionTime);
    }

    public void UpdateLastExecutionTime()
    {
        lastExecutionTime = nextExecutionTime;
    }

    public int CompareTo(ScheduledTask other)
    {
        if (other == null) return 1;
        if (nextExecutionTime == null && other.nextExecutionTime == null) return 0;
        if (nextExecutionTime == null) return 1;
        if (other.nextExecutionTime == null) return -1;
        return nextExecutionTime.Value.CompareTo(other.nextExecutionTime.Value);
    }

    public string GetId() => id;
    public TaskBase GetTask() => task;
    public DateTime? GetNextExecutionTime() => nextExecutionTime;
    public bool HasMoreExecutions() => nextExecutionTime != null;
}






using System;
using System.Collections.Generic;
using System.Threading;

public class TaskSchedulerDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the facade and observers
        TaskSchedulerService scheduler = TaskSchedulerService.GetInstance();
        scheduler.AddObserver(new LoggingObserver());

        // 2. Initialize the scheduler
        scheduler.Initialize(10);

        // 3. Define tasks and strategies
        // Scenario 1: One-time task, 1 second from now
        TaskBase oneTimeTask = new PrintMessageTask("This is a one-time task.");
        SchedulingStrategy oneTimeStrategy = new OneTimeSchedulingStrategy(DateTime.Now.AddSeconds(1));

        // Scenario 2: Recurring task, every 2 seconds
        TaskBase recurringTask = new PrintMessageTask("This is a recurring task.");
        SchedulingStrategy recurringStrategy = new RecurringSchedulingStrategy(TimeSpan.FromSeconds(2));

        // Scenario 3: A long-running backup task, scheduled to run in 3 seconds
        TaskBase backupTask = new DataBackupTask("/data/source", "/data/backup");
        SchedulingStrategy longRunningStrategy = new OneTimeSchedulingStrategy(DateTime.Now.AddSeconds(3));

        // 4. Schedule the tasks using the facade
        Console.WriteLine("Scheduling tasks...");
        scheduler.Schedule(oneTimeTask, oneTimeStrategy);
        scheduler.Schedule(recurringTask, recurringStrategy);
        scheduler.Schedule(backupTask, longRunningStrategy);

        // 5. Let the demo run for a while
        Console.WriteLine("Scheduler is running. Waiting for tasks to execute... (Demo will run for 6 seconds)");
        Thread.Sleep(6000);

        // 6. Shutdown the scheduler
        scheduler.Shutdown();
    }
}








class TaskSchedulerService
{
    private static TaskSchedulerService instance;
    private static readonly object lockObject = new object();
    
    private readonly PriorityQueue<ScheduledTask, DateTime> taskQueue = new PriorityQueue<ScheduledTask, DateTime>();
    private readonly List<TaskExecutionObserver> observers = new List<TaskExecutionObserver>();
    private readonly List<Thread> workers = new List<Thread>();
    private volatile bool running = true;
    private readonly object queueLock = new object();
    private readonly ManualResetEventSlim queueEvent = new ManualResetEventSlim(false);

    private TaskSchedulerService() { }

    public static TaskSchedulerService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new TaskSchedulerService();
                }
            }
        }
        return instance;
    }

    public void Initialize(int workerCount)
    {
        if (workerCount <= 0)
        {
            throw new ArgumentException("Worker count must be >= 1");
        }
        StartWorkers(workerCount);
    }

    public string Schedule(TaskBase task, SchedulingStrategy strategy)
    {
        ScheduledTask scheduledTask = new ScheduledTask(task, strategy);
        
        lock (queueLock)
        {
            taskQueue.Enqueue(scheduledTask, scheduledTask.GetNextExecutionTime() ?? DateTime.MaxValue);
        }
        queueEvent.Set();
        
        return scheduledTask.GetId();
    }

    private void StartWorkers(int workerCount)
    {
        for (int i = 0; i < workerCount; i++)
        {
            Thread worker = new Thread(RunWorker)
            {
                Name = $"WorkerThread-{i}",
                IsBackground = true
            };
            workers.Add(worker);
            worker.Start();
        }
    }

    private void RunWorker()
    {
        while (running)
        {
            ScheduledTask task = null;
            
            lock (queueLock)
            {
                if (taskQueue.Count > 0)
                {
                    task = taskQueue.Dequeue();
                }
            }

            if (task == null)
            {
                queueEvent.Wait(1000);
                queueEvent.Reset();
                continue;
            }

            DateTime now = DateTime.Now;
            DateTime? executionTime = task.GetNextExecutionTime();
            
            if (executionTime.HasValue && executionTime.Value > now)
            {
                TimeSpan waitTime = executionTime.Value - now;
                if (waitTime.TotalMilliseconds > 0)
                {
                    Thread.Sleep(waitTime);
                }
            }

            // Check if a higher-priority task has arrived while we were sleeping
            lock (queueLock)
            {
                if (taskQueue.Count > 0)
                {
                    if (taskQueue.Peek().CompareTo(task) < 0)
                    {
                        taskQueue.Enqueue(task, task.GetNextExecutionTime() ?? DateTime.MaxValue);
                        queueEvent.Set();
                        continue;
                    }
                }
            }

            Execute(task);
        }

        Console.WriteLine($"{Thread.CurrentThread.Name} stopped.");
    }

    private void Execute(ScheduledTask task)
    {
        foreach (TaskExecutionObserver observer in observers)
        {
            observer.OnTaskStarted(task);
        }

        try
        {
            task.GetTask().Execute();
            task.UpdateLastExecutionTime();
            foreach (TaskExecutionObserver observer in observers)
            {
                observer.OnTaskCompleted(task);
            }
        }
        catch (Exception e)
        {
            foreach (TaskExecutionObserver observer in observers)
            {
                observer.OnTaskFailed(task, e);
            }
            Console.WriteLine($"Task {task.GetId()} failed with error: {e.Message}");
        }
        finally
        {
            task.UpdateNextExecutionTime();

            if (task.HasMoreExecutions())
            {
                lock (queueLock)
                {
                    taskQueue.Enqueue(task, task.GetNextExecutionTime() ?? DateTime.MaxValue);
                }
                queueEvent.Set();
            }
            else
            {
                Console.WriteLine($"Task {task.GetId()} has no more executions and will not be rescheduled.");
            }
        }
    }

    public void Shutdown()
    {
        running = false;
        queueEvent.Set();
        
        foreach (Thread worker in workers)
        {
            worker.Join(1000);
        }
        
        Console.WriteLine("Scheduler shut down.");
    }

    public void AddObserver(TaskExecutionObserver observer)
    {
        observers.Add(observer);
    }
}

























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































