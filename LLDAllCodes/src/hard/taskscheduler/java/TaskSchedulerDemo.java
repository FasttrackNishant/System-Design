package easy.snakeandladder.java;

class LoggingObserver implements TaskExecutionObserver {
    @Override
    public void onTaskStarted(ScheduledTask task) {
        System.out.printf("[LOG - %s] [%s] Task %s started.%n", LocalTime.now(), Thread.currentThread().getName(), task.getId());
    }

    @Override
    public void onTaskCompleted(ScheduledTask task) {
        System.out.printf("[LOG - %s] [%s] Task %s completed successfully.%n", LocalTime.now(), Thread.currentThread().getName(), task.getId());
    }

    @Override
    public void onTaskFailed(ScheduledTask task, Exception e) {
        System.err.printf("[LOG - %s] [%s] Task %s failed: %s%n", LocalTime.now(), Thread.currentThread().getName(), task.getId(), e.getMessage());
    }
}




interface TaskExecutionObserver {
    void onTaskStarted(ScheduledTask task);
    void onTaskCompleted(ScheduledTask task);
    void onTaskFailed(ScheduledTask task, Exception e);
}






class OneTimeSchedulingStrategy implements SchedulingStrategy {
    private final LocalDateTime executionTime;

    public OneTimeSchedulingStrategy(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    @Override
    public Optional<LocalDateTime> getNextExecutionTime(LocalDateTime lastExecutionTime) {
        // If lastExecutionTime is null, it's the first run. Otherwise, it's done.
        return (lastExecutionTime == null) ? Optional.of(executionTime) : Optional.empty();
    }
}




class RecurringSchedulingStrategy implements SchedulingStrategy {
    private final Duration interval;

    public RecurringSchedulingStrategy(Duration interval) {
        this.interval = interval;
    }

    @Override
    public Optional<LocalDateTime> getNextExecutionTime(LocalDateTime lastExecutionTime) {
        // If first run, schedule from now. Otherwise, schedule from the last execution time.
        LocalDateTime baseTime = (lastExecutionTime == null) ? LocalDateTime.now() : lastExecutionTime;
        return Optional.of(baseTime.plus(interval));
    }
}



interface SchedulingStrategy {
    Optional<LocalDateTime> getNextExecutionTime(LocalDateTime lastExecutionTime);
}









class DataBackupTask implements Task {
    private final String source;
    private final String destination;

    public DataBackupTask(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public void execute() {
        System.out.printf("[%s] Executing DataBackupTask: Backing up from %s to %s...%n", LocalTime.now(), source, destination);
        // Simulate a long-running task
        System.out.printf("[%s] DataBackupTask: Backup complete.%n", LocalTime.now());
    }
}





class PrintMessageTask implements Task {
    private final String message;

    public PrintMessageTask(String message) {
        this.message = message;
    }

    @Override
    public void execute() {
        System.out.printf("[%s] Executing PrintMessageTask: %s%n", LocalTime.now(), message);
    }
}



interface Task {
    void execute();
}






class ScheduledTask implements Comparable<ScheduledTask> {
    private final String id;
    private final Task task;
    private final SchedulingStrategy strategy;
    private LocalDateTime nextExecutionTime;
    private LocalDateTime lastExecutionTime;

    public ScheduledTask(Task task, SchedulingStrategy strategy) {
        this.id = UUID.randomUUID().toString();
        this.task = task;
        this.strategy = strategy;
        updateNextExecutionTime();
    }

    public void updateNextExecutionTime() {
        Optional<LocalDateTime> nextTime = strategy.getNextExecutionTime(this.lastExecutionTime);
        this.nextExecutionTime = nextTime.orElse(null);
    }

    public void updateLastExecutionTime() {
        this.lastExecutionTime = nextExecutionTime;
    }

    @Override
    public int compareTo(ScheduledTask other) {
        return this.nextExecutionTime.compareTo(other.nextExecutionTime);
    }

    // Getters
    public String getId() { return id; }
    public Task getTask() { return task; }
    public LocalDateTime getNextExecutionTime() { return nextExecutionTime; }
    public boolean hasMoreExecutions() { return nextExecutionTime != null; }
}






import java.util.*;
import java.time.*;
import java.util.concurrent.PriorityBlockingQueue;

public class TaskSchedulerDemo {
    public static void main(String[] args) throws InterruptedException {
        // 1. Setup the facade and observers
        TaskSchedulerService scheduler = TaskSchedulerService.getInstance();
        scheduler.addObserver(new LoggingObserver());

        // 2. Initialize the scheduler
        scheduler.initialize(10);

        // 3. Define tasks and strategies
        // Scenario 1: One-time task, 5 seconds from now
        Task oneTimeTask = new PrintMessageTask("This is a one-time task.");
        SchedulingStrategy oneTimeStrategy = new OneTimeSchedulingStrategy(LocalDateTime.now().plusSeconds(1));

        // Scenario 2: Recurring task, every 3 seconds
        Task recurringTask = new PrintMessageTask("This is a recurring task.");
        SchedulingStrategy recurringStrategy = new RecurringSchedulingStrategy(Duration.ofSeconds(2));

        // Scenario 3: A long-running backup task, scheduled to run in 1 second
        Task backupTask = new DataBackupTask("/data/source", "/data/backup");
        SchedulingStrategy longRunningRecurringStrategy = new OneTimeSchedulingStrategy(LocalDateTime.now().plusSeconds(3));

        // 4. Schedule the tasks using the facade
        System.out.println("Scheduling tasks...");
        scheduler.schedule(oneTimeTask, oneTimeStrategy);
        scheduler.schedule(recurringTask, recurringStrategy);
        scheduler.schedule(backupTask, longRunningRecurringStrategy);

        // 5. Let the demo run for a while
        System.out.println("Scheduler is running. Waiting for tasks to execute... (Demo will run for 10 seconds)");
        Thread.sleep(6000);

        // 6. Shutdown the scheduler
        scheduler.shutdown();
    }
}






class TaskSchedulerService {
    private static final TaskSchedulerService INSTANCE = new TaskSchedulerService();
    private final PriorityBlockingQueue<ScheduledTask> taskQueue = new PriorityBlockingQueue<>();
    private final List<TaskExecutionObserver> observers = new ArrayList<>();
    private Thread[] workers;
    private volatile boolean running = true;

    private TaskSchedulerService() {}

    public static TaskSchedulerService getInstance() {
        return INSTANCE;
    }

    public void initialize(int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be >= 1");
        }
        workers = new Thread[workerCount];
        startWorkers();
    }

    public String schedule(Task task, SchedulingStrategy strategy) {
        ScheduledTask scheduledTask = new ScheduledTask(task, strategy);
        taskQueue.put(scheduledTask);
        return scheduledTask.getId();
    }

    private void startWorkers() {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(this::runWorker, "WorkerThread-" + i);
            workers[i].setDaemon(true);
            workers[i].start();
        }
    }

    private void runWorker() {
        while (running) {
            try {
                // take() blocks until an element is available.
                ScheduledTask task = taskQueue.take();
                LocalDateTime now = LocalDateTime.now();
                long waitTime = 0;

                if (task.getNextExecutionTime().isAfter(now)) {
                    waitTime = Duration.between(now, task.getNextExecutionTime()).toMillis();
                }

                if (waitTime > 0) {
                    // Wait for the scheduled time.
                    Thread.sleep(waitTime);
                }

                // Check if a higher-priority task has arrived while we were sleeping
                ScheduledTask head = taskQueue.peek();
                if (head != null && head.compareTo(task) < 0) {
                    taskQueue.put(task); // Put our task back and let the higher-priority one run
                    continue;
                }

                // --- Execute the task ---
                execute(task);
            } catch (InterruptedException e) {
                // This is the expected way to stop the worker thread.
                Thread.currentThread().interrupt();
                break; // Exit the loop
            }
        }
        System.out.printf("%s stopped.%n", Thread.currentThread().getName());
    }

    private void execute(ScheduledTask task) {
        observers.forEach(o -> o.onTaskStarted(task));
        try {
            task.getTask().execute();
            task.updateLastExecutionTime();
            observers.forEach(o -> o.onTaskCompleted(task));
        } catch (Exception e) {
            observers.forEach(o -> o.onTaskFailed(task, e));
            System.err.printf("Task %s failed with error: %s%n", task.getId(), e.getMessage());
        } finally {
            // --- Re-scheduling logic ---
            // Must be done whether the task succeeded or failed.
            task.updateNextExecutionTime();

            if (task.hasMoreExecutions()) {
                taskQueue.put(task); // Re-queue for the next run.
            } else {
                System.out.printf("Task %s has no more executions and will not be rescheduled.%n", task.getId());
            }
        }
    }

    public void shutdown() {
        running = false;
        for (Thread worker : workers) {
            worker.interrupt(); // in case they're blocked on take()
        }
        System.out.println("Scheduler shut down.");
    }

    public void addObserver(TaskExecutionObserver observer) {
        observers.add(observer);
    }
}




































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































