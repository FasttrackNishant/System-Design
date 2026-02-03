


class ActivityLog {
private:
    string description;
    time_t timestamp;

public:
    ActivityLog(const string& description) : description(description) {
        timestamp = time(nullptr);
    }

    string toString() const {
        stringstream ss;
        ss << "[" << put_time(localtime(&timestamp), "%Y-%m-%d %H:%M:%S") << "] " << description;
        return ss.str();
    }
};





class Comment {
private:
    string id;
    string content;
    User* author;
    time_t timestamp;

public:
    Comment(const string& content, User* author) : content(content), author(author) {
        id = "comment_" + to_string(rand());
        timestamp = time(nullptr);
    }

    User* getAuthor() const { return author; }
};




class Tag {
private:
    string name;

public:
    Tag(const string& name) : name(name) {}
    string getName() const { return name; }
};





class Task {
public:
    class TaskBuilder;
    
private:
    string id;
    string title;
    string description;
    string dueDate; // Using string for simplicity
    TaskPriority priority;
    User* createdBy;
    User* assignee;
    TaskState* currentState;
    set<Tag*> tags;
    vector<Comment*> comments;
    vector<Task*> subtasks;
    vector<ActivityLog*> activityLogs;
    vector<TaskObserver*> observers;
    mutex taskMutex;

    Task(TaskBuilder* builder);

public:
    void setAssignee(User* user) {
        lock_guard<mutex> lock(taskMutex);
        assignee = user;
        addLog("Assigned to " + user->getName());
        notifyObservers("assignee");
    }

    void updatePriority(TaskPriority priority) {
        lock_guard<mutex> lock(taskMutex);
        this->priority = priority;
        notifyObservers("priority");
    }

    void addComment(Comment* comment) {
        lock_guard<mutex> lock(taskMutex);
        comments.push_back(comment);
        addLog("Comment added by " + comment->getAuthor()->getName());
        notifyObservers("comment");
    }

    void addSubtask(Task* subtask) {
        lock_guard<mutex> lock(taskMutex);
        subtasks.push_back(subtask);
        addLog("Subtask added: " + subtask->getTitle());
        notifyObservers("subtask_added");
    }

    // State Pattern Methods
    void setState(TaskState* state) {
        delete currentState;
        currentState = state;
        addLog("Status changed to: " + getStatusString());
        notifyObservers("status");
    }

    void startProgress();
    void completeTask();
    void reopenTask();

    // Observer Pattern Methods
    void addObserver(TaskObserver* observer) { observers.push_back(observer); }
    void removeObserver(TaskObserver* observer) {
        auto it = find(observers.begin(), observers.end(), observer);
        if (it != observers.end()) {
            observers.erase(it);
        }
    }

    void notifyObservers(const string& changeType) {
        for (TaskObserver* observer : observers) {
            observer->update(this, changeType);
        }
    }

    void addLog(const string& logDescription) {
        activityLogs.push_back(new ActivityLog(logDescription));
    }

    bool isComposite() const { return !subtasks.empty(); }

    void display(const string& indent = "") const {
        cout << indent << "- " << title << " [" << getStatusString() 
             << ", " << getPriorityString() << ", Due: " << dueDate << "]" << endl;
        if (isComposite()) {
            for (Task* subtask : subtasks) {
                subtask->display(indent + "  ");
            }
        }
    }

    // Getters
    string getId() const { return id; }
    string getTitle() const { return title; }
    string getDescription() const { return description; }
    TaskPriority getPriority() const { return priority; }
    string getDueDate() const { return dueDate; }
    User* getAssignee() const { return assignee; }
    TaskStatus getStatus() const;

    void setTitle(const string& title) { this->title = title; }
    void setDescription(const string& description) { this->description = description; }

    string getStatusString() const {
        switch (getStatus()) {
            case TaskStatus::TODO: return "TODO";
            case TaskStatus::IN_PROGRESS: return "IN_PROGRESS";
            case TaskStatus::DONE: return "DONE";
            case TaskStatus::BLOCKED: return "BLOCKED";
            default: return "UNKNOWN";
        }
    }

    string getPriorityString() const {
        switch (priority) {
            case TaskPriority::LOW: return "LOW";
            case TaskPriority::MEDIUM: return "MEDIUM";
            case TaskPriority::HIGH: return "HIGH";
            case TaskPriority::CRITICAL: return "CRITICAL";
            default: return "UNKNOWN";
        }
    }

    // Builder Pattern
    class TaskBuilder {
    public:
        string id;
        string title;
        string description;
        string dueDate;
        TaskPriority priority;
        User* createdBy;
        User* assignee;
        set<Tag*> tags;

        TaskBuilder(const string& title) : title(title) {
            id = "task_" + to_string(rand());
            description = "";
            createdBy = nullptr;
            assignee = nullptr;
        }

        TaskBuilder& setDescription(const string& description) {
            this->description = description;
            return *this;
        }

        TaskBuilder& setDueDate(const string& dueDate) {
            this->dueDate = dueDate;
            return *this;
        }

        TaskBuilder& setPriority(TaskPriority priority) {
            this->priority = priority;
            return *this;
        }

        TaskBuilder& setAssignee(User* assignee) {
            this->assignee = assignee;
            return *this;
        }

        TaskBuilder& setCreatedBy(User* createdBy) {
            this->createdBy = createdBy;
            return *this;
        }

        TaskBuilder& setTags(const set<Tag*>& tags) {
            this->tags = tags;
            return *this;
        }

        Task* build() {
            return new Task(this);
        }
    };

    ~Task() {
        delete currentState;
        for (ActivityLog* log : activityLogs) {
            delete log;
        }
    }
};









class TaskList {
private:
    string id;
    string name;
    vector<Task*> tasks;
    mutex listMutex;

public:
    TaskList(const string& name) : name(name) {
        id = "list_" + to_string(rand());
    }

    void addTask(Task* task) {
        lock_guard<mutex> lock(listMutex);
        tasks.push_back(task);
    }

    vector<Task*> getTasks() {
        lock_guard<mutex> lock(listMutex);
        return tasks; // Return copy
    }

    string getId() const { return id; }
    string getName() const { return name; }

    void display() const {
        cout << "--- Task List: " << name << " ---" << endl;
        for (Task* task : tasks) {
            task->display("");
        }
        cout << "-----------------------------------" << endl;
    }
};







class User {
private:
    string id;
    string name;
    string email;

public:
    User(const string& name, const string& email) : name(name), email(email) {
        id = "user_" + to_string(rand()); // Simple ID generation
    }

    string getId() const { return id; }
    string getEmail() const { return email; }
    string getName() const { return name; }
};





enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
};





enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
};




class ActivityLogger : public TaskObserver {
public:
    void update(Task* task, const string& changeType) override {
        cout << "LOGGER: Task '" << task->getTitle() << "' was updated. Change: " << changeType << endl;        
    }
};


class TaskObserver {
public:
    virtual ~TaskObserver() = default;
    virtual void update(Task* task, const string& changeType) = 0;
};





class DoneState : public TaskState {
public:
    void startProgress(Task* task) override {
        cout << "Cannot start a completed task. Reopen it first." << endl;
    }
    void completeTask(Task* task) override {
        cout << "Task is already done." << endl;
    }
    void reopenTask(Task* task) override;
    TaskStatus getStatus() override { return TaskStatus::DONE; }
};

Task::Task(TaskBuilder* builder) {
    id = builder->id;
    title = builder->title;
    description = builder->description;
    dueDate = builder->dueDate;
    priority = builder->priority;
    createdBy = builder->createdBy;
    assignee = builder->assignee;
    tags = builder->tags;
    currentState = new TodoState(); // Initial state
    addLog("Task created with title: " + title);
}

TaskStatus Task::getStatus() const { 
    return currentState->getStatus(); 
}

void Task::startProgress() { 
    currentState->startProgress(this); 
}

void Task::completeTask() { 
    currentState->completeTask(this); 
}

void Task::reopenTask() { 
    currentState->reopenTask(this); 
}

// State method implementations
void TodoState::startProgress(Task* task) {
    task->setState(new InProgressState());
}

void InProgressState::completeTask(Task* task) {
    task->setState(new DoneState());
}

void InProgressState::reopenTask(Task* task) {
    task->setState(new TodoState());
}

void DoneState::reopenTask(Task* task) {
    task->setState(new TodoState());
}












class InProgressState : public TaskState {
public:
    void startProgress(Task* task) override {
        cout << "Task is already in progress." << endl;
    }
    void completeTask(Task* task) override;
    void reopenTask(Task* task) override;
    TaskStatus getStatus() override { return TaskStatus::IN_PROGRESS; }
};












class TaskState {
public:
    virtual ~TaskState() = default;
    virtual void startProgress(Task* task) = 0;
    virtual void completeTask(Task* task) = 0;
    virtual void reopenTask(Task* task) = 0;
    virtual TaskStatus getStatus() = 0;
};








class TodoState : public TaskState {
public:
    void startProgress(Task* task) override;
    void completeTask(Task* task) override {
        cout << "Cannot complete a task that is not in progress." << endl;
    }
    void reopenTask(Task* task) override {
        cout << "Task is already in TO-DO state." << endl;
    }
    TaskStatus getStatus() override { return TaskStatus::TODO; }
};











class SortByDueDate : public TaskSortStrategy {
public:
    void sort(vector<Task*>& tasks) override {
        std::sort(tasks.begin(), tasks.end(), [](Task* a, Task* b) {
            return a->getDueDate() < b->getDueDate();
        });
    }
};






class SortByPriority : public TaskSortStrategy {
public:
    void sort(vector<Task*>& tasks) override {
        // Higher priority comes first
        std::sort(tasks.begin(), tasks.end(), [](Task* a, Task* b) {
            return static_cast<int>(a->getPriority()) > static_cast<int>(b->getPriority());
        });
    }
};






class TaskSortStrategy {
public:
    virtual ~TaskSortStrategy() = default;
    virtual void sort(vector<Task*>& tasks) = 0;
};








class TaskManagementSystem {
private:
    static TaskManagementSystem* instance;
    static mutex instanceMutex;
    map<string, User*> users;
    map<string, Task*> tasks;
    map<string, TaskList*> taskLists;

    TaskManagementSystem() {}

public:
    static TaskManagementSystem* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new TaskManagementSystem();
        }
        return instance;
    }

    User* createUser(const string& name, const string& email) {
        User* user = new User(name, email);
        users[user->getId()] = user;
        return user;
    }

    TaskList* createTaskList(const string& listName) {
        TaskList* taskList = new TaskList(listName);
        taskLists[taskList->getId()] = taskList;
        return taskList;
    }

    Task* createTask(const string& title, const string& description, const string& dueDate,
                     TaskPriority priority, const string& createdByUserId) {
        User* createdBy = users[createdByUserId];
        if (createdBy == nullptr) {
            throw invalid_argument("User not found.");
        }

        Task* task = Task::TaskBuilder(title)
                .setDescription(description)
                .setDueDate(dueDate)
                .setPriority(priority)
                .setCreatedBy(createdBy)
                .build();

        task->addObserver(new ActivityLogger());

        tasks[task->getId()] = task;
        return task;
    }

    vector<Task*> listTasksByUser(const string& userId) {
        User* user = users[userId];
        vector<Task*> result;
        for (auto& pair : tasks) {
            if (pair.second->getAssignee() == user) {
                result.push_back(pair.second);
            }
        }
        return result;
    }

    vector<Task*> listTasksByStatus(TaskStatus status) {
        vector<Task*> result;
        for (auto& pair : tasks) {
            if (pair.second->getStatus() == status) {
                result.push_back(pair.second);
            }
        }
        return result;
    }

    void deleteTask(const string& taskId) {
        auto it = tasks.find(taskId);
        if (it != tasks.end()) {
            delete it->second;
            tasks.erase(it);
        }
    }

    vector<Task*> searchTasks(const string& keyword, TaskSortStrategy* sortingStrategy) {
        vector<Task*> matchingTasks;
        for (auto& pair : tasks) {
            Task* task = pair.second;
            if (task->getTitle().find(keyword) != string::npos || 
                task->getDescription().find(keyword) != string::npos) {
                matchingTasks.push_back(task);
            }
        }
        sortingStrategy->sort(matchingTasks);
        return matchingTasks;
    }
};

TaskManagementSystem* TaskManagementSystem::instance = nullptr;
mutex TaskManagementSystem::instanceMutex;
























int main() {
    TaskManagementSystem* taskManagementSystem = TaskManagementSystem::getInstance();

    // Create users
    User* user1 = taskManagementSystem->createUser("John Doe", "john@example.com");
    User* user2 = taskManagementSystem->createUser("Jane Smith", "jane@example.com");

    // Create task lists
    TaskList* taskList1 = taskManagementSystem->createTaskList("Enhancements");
    TaskList* taskList2 = taskManagementSystem->createTaskList("Bug Fix");

    // Create tasks
    Task* task1 = taskManagementSystem->createTask("Enhancement Task", "Launch New Feature",
            "2024-02-15", TaskPriority::LOW, user1->getId());
    Task* subtask1 = taskManagementSystem->createTask("Enhancement sub task", "Design UI/UX",
            "2024-02-14", TaskPriority::MEDIUM, user1->getId());
    Task* task2 = taskManagementSystem->createTask("Bug Fix Task", "Fix API Bug",
            "2024-02-16", TaskPriority::HIGH, user2->getId());

    task1->addSubtask(subtask1);

    taskList1->addTask(task1);
    taskList2->addTask(task2);

    taskList1->display();

    // Update task status
    subtask1->startProgress();

    // Assign task
    subtask1->setAssignee(user2);

    taskList1->display();

    // Search tasks
    vector<Task*> searchResults = taskManagementSystem->searchTasks("Task", new SortByDueDate());
    cout << "\nTasks with keyword Task:" << endl;
    for (Task* task : searchResults) {
        cout << task->getTitle() << endl;
    }

    // Filter tasks by status
    vector<Task*> filteredTasks = taskManagementSystem->listTasksByStatus(TaskStatus::TODO);
    cout << "\nTODO Tasks:" << endl;
    for (Task* task : filteredTasks) {
        cout << task->getTitle() << endl;
    }

    // Mark a task as done
    subtask1->completeTask();

    // Get tasks assigned to a user
    vector<Task*> userTaskList = taskManagementSystem->listTasksByUser(user2->getId());
    cout << "\nTask for " << user2->getName() << ":" << endl;
    for (Task* task : userTaskList) {
        cout << task->getTitle() << endl;
    }

    taskList1->display();

    // Delete a task
    taskManagementSystem->deleteTask(task2->getId());

    return 0;
}





































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































