class Task {
public:
    virtual ~Task() = default;
    virtual void execute() = 0;
};

class PrintMessageTask : public Task {
private:
    string message;

public:
    PrintMessageTask(const string& msg) : message(msg) {}

    void execute() override {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        auto ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cout << "[" << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "Executing PrintMessageTask: " << message << endl;
    }
};

class DataBackupTask : public Task {
private:
    string source;
    string destination;

public:
    DataBackupTask(const string& src, const string& dest) : source(src), destination(dest) {}

    void execute() override {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        auto ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cout << "[" << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "Executing DataBackupTask: Backing up from " << source << " to " << destination << "..." << endl;
        
        now = chrono::system_clock::now();
        time_t = chrono::system_clock::to_time_t(now);
        ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cout << "[" << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "DataBackupTask: Backup complete." << endl;
    }
};








class ScheduledTask {
private:
    string id;
    unique_ptr<Task> task;
    unique_ptr<SchedulingStrategy> strategy;
    chrono::system_clock::time_point nextExecutionTime;
    chrono::system_clock::time_point* lastExecutionTime;

    string generateUUID() {
        // Simple UUID generation (not cryptographically secure)
        srand(time(nullptr));
        stringstream ss;
        ss << hex;
        for (int i = 0; i < 8; i++) {
            ss << (rand() % 16);
        }
        ss << "-";
        for (int i = 0; i < 4; i++) {
            ss << (rand() % 16);
        }
        ss << "-4";
        for (int i = 0; i < 3; i++) {
            ss << (rand() % 16);
        }
        ss << "-";
        ss << ((rand() % 4) + 8);
        for (int i = 0; i < 3; i++) {
            ss << (rand() % 16);
        }
        ss << "-";
        for (int i = 0; i < 12; i++) {
            ss << (rand() % 16);
        }
        return ss.str();
    }

public:
    ScheduledTask(unique_ptr<Task> t, unique_ptr<SchedulingStrategy> s) 
        : task(move(t)), strategy(move(s)), lastExecutionTime(nullptr) {
        id = generateUUID();
        updateNextExecutionTime();
    }

    ~ScheduledTask() {
        delete lastExecutionTime;
    }

    void updateNextExecutionTime() {
        nextExecutionTime = strategy->getNextExecutionTime(lastExecutionTime);
    }

    void updateLastExecutionTime() {
        delete lastExecutionTime;
        lastExecutionTime = new chrono::system_clock::time_point(nextExecutionTime);
    }

    bool operator<(const ScheduledTask& other) const {
        return nextExecutionTime > other.nextExecutionTime; // Reversed for min-heap
    }

    bool hasMoreExecutions() const { return strategy->hasNext(lastExecutionTime); }

    // Getters
};
























class SchedulingStrategy {
public:
    virtual ~SchedulingStrategy() = default;
    virtual chrono::system_clock::time_point getNextExecutionTime(
        const chrono::system_clock::time_point* lastExecutionTime) = 0;
    virtual bool hasNext(const chrono::system_clock::time_point* lastExecutionTime) = 0;
};

class OneTimeSchedulingStrategy : public SchedulingStrategy {
private:
    chrono::system_clock::time_point executionTime;

public:
    OneTimeSchedulingStrategy(const chrono::system_clock::time_point& execTime) 
        : executionTime(execTime) {}

    chrono::system_clock::time_point getNextExecutionTime(
        const chrono::system_clock::time_point* lastExecutionTime) override {
        return executionTime;
    }

    bool hasNext(const chrono::system_clock::time_point* lastExecutionTime) override {
        return lastExecutionTime == nullptr;
    }
};

class RecurringSchedulingStrategy : public SchedulingStrategy {
private:
    chrono::duration<long long> interval;

public:
    RecurringSchedulingStrategy(const chrono::duration<long long>& intervalDuration) 
        : interval(intervalDuration) {}

    chrono::system_clock::time_point getNextExecutionTime(
        const chrono::system_clock::time_point* lastExecutionTime) override {
        chrono::system_clock::time_point baseTime = 
            (lastExecutionTime == nullptr) ? chrono::system_clock::now() : *lastExecutionTime;
        return baseTime + interval;
    }

    bool hasNext(const chrono::system_clock::time_point* lastExecutionTime) override {
        return true;
    }
};













class TaskExecutionObserver {
public:
    virtual ~TaskExecutionObserver() = default;
    virtual void onTaskStarted(ScheduledTask* task) = 0;
    virtual void onTaskCompleted(ScheduledTask* task) = 0;
    virtual void onTaskFailed(ScheduledTask* task, const exception& e) = 0;
};

class LoggingObserver : public TaskExecutionObserver {
public:
    void onTaskStarted(ScheduledTask* task) override {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        auto ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cout << "[LOG - " << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "[" << this_thread::get_id() << "] Task " << task->getId() << " started." << endl;
    }

    void onTaskCompleted(ScheduledTask* task) override {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        auto ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cout << "[LOG - " << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "[" << this_thread::get_id() << "] Task " << task->getId() << " completed successfully." << endl;
    }

    void onTaskFailed(ScheduledTask* task, const exception& e) override {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        auto ms = chrono::duration_cast<chrono::milliseconds>(now.time_since_epoch()) % 1000;
        
        cerr << "[LOG - " << put_time(localtime(&time_t), "%H:%M:%S") << "." 
             << setfill('0') << setw(3) << ms.count() << "] "
             << "[" << this_thread::get_id() << "] Task " << task->getId() << " failed: " << e.what() << endl;
    }
};




















class TaskSchedulerService {
private:
    static TaskSchedulerService* instance;
    static mutex instanceMutex;
    
    priority_queue<unique_ptr<ScheduledTask>> taskQueue;
    vector<unique_ptr<TaskExecutionObserver>> observers;
    vector<thread> workers;
    atomic<bool> running;
    mutex queueMutex;
    condition_variable queueCondition;

    TaskSchedulerService() : running(true) {}

public:
    static TaskSchedulerService* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new TaskSchedulerService();
        }
        return instance;
    }

    void initialize(int workerCount) {
        if (workerCount <= 0) {
            throw invalid_argument("Worker count must be >= 1");
        }
        startWorkers(workerCount);
    }

    string schedule(unique_ptr<Task> task, unique_ptr<SchedulingStrategy> strategy) {
        auto scheduledTask = make_unique<ScheduledTask>(move(task), move(strategy));
        string taskId = scheduledTask->getId();
        
        {
            lock_guard<mutex> lock(queueMutex);
            taskQueue.push(move(scheduledTask));
        }
        queueCondition.notify_one();
        
        return taskId;
    }

private:
    void startWorkers(int workerCount) {
        for (int i = 0; i < workerCount; i++) {
            workers.emplace_back(&TaskSchedulerService::runWorker, this);
        }
    }

    void runWorker() {
        while (running) {
            unique_ptr<ScheduledTask> task;
            
            {
                unique_lock<mutex> lock(queueMutex);
                queueCondition.wait(lock, [this] { return !taskQueue.empty() || !running; });
                
                if (!running) break;
                
                task = move(const_cast<unique_ptr<ScheduledTask>&>(taskQueue.top()));
                taskQueue.pop();
            }

            auto now = chrono::system_clock::now();
            auto waitTime = task->getNextExecutionTime() - now;

            if (waitTime > chrono::milliseconds(0)) {
                this_thread::sleep_for(waitTime);
            }

            // Check if a higher-priority task has arrived while we were sleeping
            {
                lock_guard<mutex> lock(queueMutex);
                if (!taskQueue.empty() && *taskQueue.top() < *task) {
                    taskQueue.push(move(task));
                    queueCondition.notify_one();
                    continue;
                }
            }

            execute(task.get());
        }
        
        cout << "WorkerThread-" << this_thread::get_id() << " stopped." << endl;
    }

    void execute(ScheduledTask* task) {
        for (auto& observer : observers) {
            observer->onTaskStarted(task);
        }

        try {
            task->getTask()->execute();
            task->updateLastExecutionTime();
            for (auto& observer : observers) {
                observer->onTaskCompleted(task);
            }
        } catch (const exception& e) {
            for (auto& observer : observers) {
                observer->onTaskFailed(task, e);
            }
            cerr << "Task " << task->getId() << " failed with error: " << e.what() << endl;
        }

        task->updateNextExecutionTime();

        if (task->hasMoreExecutions()) {
            lock_guard<mutex> lock(queueMutex);
            taskQueue.push(make_unique<ScheduledTask>(*task));
            queueCondition.notify_one();
        } else {
            cout << "Task " << task->getId() << " has no more executions and will not be rescheduled." << endl;
        }
    }

public:
    void shutdown() {
        running = false;
        queueCondition.notify_all();
        
        for (auto& worker : workers) {
            if (worker.joinable()) {
                worker.join();
            }
        }
        
        cout << "Scheduler shut down." << endl;
    }

    void addObserver(unique_ptr<TaskExecutionObserver> observer) {
        observers.push_back(move(observer));
    }
};










int main() {
    // 1. Setup the facade and observers
    TaskSchedulerService* scheduler = TaskSchedulerService::getInstance();
    scheduler->addObserver(make_unique<LoggingObserver>());

    // 2. Initialize the scheduler
    scheduler->initialize(10);

    // 3. Define tasks and strategies
    // Scenario 1: One-time task, 1 second from now
    auto oneTimeTask = make_unique<PrintMessageTask>("This is a one-time task.");
    auto oneTimeStrategy = make_unique<OneTimeSchedulingStrategy>(
        chrono::system_clock::now() + chrono::seconds(1));

    // Scenario 2: Recurring task, every 2 seconds
    auto recurringTask = make_unique<PrintMessageTask>("This is a recurring task.");
    auto recurringStrategy = make_unique<RecurringSchedulingStrategy>(chrono::seconds(2));

    // Scenario 3: A long-running backup task, scheduled to run in 3 seconds
    auto backupTask = make_unique<DataBackupTask>("/data/source", "/data/backup");
    auto longRunningStrategy = make_unique<OneTimeSchedulingStrategy>(
        chrono::system_clock::now() + chrono::seconds(3));

    // 4. Schedule the tasks using the facade
    cout << "Scheduling tasks..." << endl;
    scheduler->schedule(move(oneTimeTask), move(oneTimeStrategy));
    scheduler->schedule(move(recurringTask), move(recurringStrategy));
    scheduler->schedule(move(backupTask), move(longRunningStrategy));

    // 5. Let the demo run for a while
    cout << "Scheduler is running. Waiting for tasks to execute... (Demo will run for 6 seconds)" << endl;
    this_thread::sleep_for(chrono::seconds(6));

    // 6. Shutdown the scheduler
    scheduler->shutdown();

    return 0;
}




















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































