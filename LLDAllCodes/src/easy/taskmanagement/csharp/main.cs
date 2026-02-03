class ActivityLog
{
    private readonly string description;
    private readonly DateTime timestamp;

    public ActivityLog(string description)
    {
        this.description = description;
        this.timestamp = DateTime.Now;
    }

    public override string ToString()
    {
        return $"[{timestamp}] {description}";
    }
}






class Comment
{
    private readonly string id;
    private readonly string content;
    private readonly User author;
    private readonly DateTime timestamp;

    public Comment(string content, User author)
    {
        this.id = Guid.NewGuid().ToString();
        this.content = content;
        this.author = author;
        this.timestamp = DateTime.Now;
    }

    public User GetAuthor() => author;
}






class Tag
{
    private readonly string name;

    public Tag(string name)
    {
        this.name = name;
    }

    public string GetName() => name;
}











class Task
{
    private readonly string id;
    private string title;
    private string description;
    private string dueDate;
    private TaskPriority priority;
    private readonly User createdBy;
    private User assignee;
    private TaskState currentState;
    private readonly HashSet<Tag> tags;
    private readonly List<Comment> comments;
    private readonly List<Task> subtasks;
    private readonly List<ActivityLog> activityLogs;
    private readonly List<ITaskObserver> observers;
    private readonly object taskLock = new object();

    public Task(TaskBuilder builder)
    {
        this.id = builder.Id;
        this.title = builder.Title;
        this.description = builder.Description;
        this.dueDate = builder.DueDate;
        this.priority = builder.Priority;
        this.createdBy = builder.CreatedBy;
        this.assignee = builder.Assignee;
        this.tags = builder.Tags;
        this.currentState = new TodoState(); // Initial state
        this.comments = new List<Comment>();
        this.subtasks = new List<Task>();
        this.activityLogs = new List<ActivityLog>();
        this.observers = new List<ITaskObserver>();
        AddLog($"Task created with title: {title}");
    }

    public void SetAssignee(User user)
    {
        lock (taskLock)
        {
            this.assignee = user;
            AddLog($"Assigned to {user.GetName()}");
            NotifyObservers("assignee");
        }
    }

    public void UpdatePriority(TaskPriority priority)
    {
        lock (taskLock)
        {
            this.priority = priority;
            NotifyObservers("priority");
        }
    }

    public void AddComment(Comment comment)
    {
        lock (taskLock)
        {
            comments.Add(comment);
            AddLog($"Comment added by {comment.GetAuthor().GetName()}");
            NotifyObservers("comment");
        }
    }

    public void AddSubtask(Task subtask)
    {
        lock (taskLock)
        {
            subtasks.Add(subtask);
            AddLog($"Subtask added: {subtask.GetTitle()}");
            NotifyObservers("subtask_added");
        }
    }

    // State Pattern Methods
    public void SetState(TaskState state)
    {
        this.currentState = state;
        AddLog($"Status changed to: {state.GetStatus()}");
        NotifyObservers("status");
    }

    public void StartProgress() => currentState.StartProgress(this);
    public void CompleteTask() => currentState.CompleteTask(this);
    public void ReopenTask() => currentState.ReopenTask(this);

    // Observer Pattern Methods
    public void AddObserver(ITaskObserver observer) => observers.Add(observer);
    public void RemoveObserver(ITaskObserver observer) => observers.Remove(observer);

    public void NotifyObservers(string changeType)
    {
        foreach (var observer in observers)
        {
            observer.Update(this, changeType);
        }
    }

    public void AddLog(string logDescription)
    {
        activityLogs.Add(new ActivityLog(logDescription));
    }

    public bool IsComposite() => subtasks.Count > 0;

    public void Display(string indent = "")
    {
        Console.WriteLine($"{indent}- {title} [{GetStatus()}, {priority}, Due: {dueDate}]");
        if (IsComposite())
        {
            foreach (var subtask in subtasks)
            {
                subtask.Display(indent + "  ");
            }
        }
    }

    // Getters
    public string GetId() => id;
    public string GetTitle() => title;
    public string GetDescription() => description;
    public TaskPriority GetPriority() => priority;
    public string GetDueDate() => dueDate;
    public User GetAssignee() => assignee;
    public TaskStatus GetStatus() => currentState.GetStatus();

    public void SetTitle(string title) => this.title = title;
    public void SetDescription(string description) => this.description = description;
}

// Builder Pattern
class TaskBuilder
{
    public string Id { get; private set; }
    public string Title { get; private set; }
    public string Description { get; private set; } = "";
    public string DueDate { get; private set; }
    public TaskPriority Priority { get; private set; }
    public User CreatedBy { get; private set; }
    public User Assignee { get; private set; }
    public HashSet<Tag> Tags { get; private set; } = new HashSet<Tag>();

    public TaskBuilder(string title)
    {
        this.Id = Guid.NewGuid().ToString();
        this.Title = title;
    }

    public TaskBuilder SetDescription(string description)
    {
        this.Description = description;
        return this;
    }

    public TaskBuilder SetDueDate(string dueDate)
    {
        this.DueDate = dueDate;
        return this;
    }

    public TaskBuilder SetPriority(TaskPriority priority)
    {
        this.Priority = priority;
        return this;
    }

    public TaskBuilder SetAssignee(User assignee)
    {
        this.Assignee = assignee;
        return this;
    }

    public TaskBuilder SetCreatedBy(User createdBy)
    {
        this.CreatedBy = createdBy;
        return this;
    }

    public TaskBuilder SetTags(HashSet<Tag> tags)
    {
        this.Tags = tags;
        return this;
    }

    public Task Build()
    {
        return new Task(this);
    }
}











class TaskList
{
    private readonly string id;
    private readonly string name;
    private readonly List<Task> tasks;
    private readonly object listLock = new object();

    public TaskList(string name)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.tasks = new List<Task>();
    }

    public void AddTask(Task task)
    {
        lock (listLock)
        {
            tasks.Add(task);
        }
    }

    public List<Task> GetTasks()
    {
        lock (listLock)
        {
            return new List<Task>(tasks); // Return copy
        }
    }

    public string GetId() => id;
    public string GetName() => name;

    public void Display()
    {
        Console.WriteLine($"--- Task List: {name} ---");
        foreach (var task in tasks)
        {
            task.Display("");
        }
        Console.WriteLine("-----------------------------------");
    }
}






class User
{
    private readonly string id;
    private readonly string name;
    private readonly string email;

    public User(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
    }

    public string GetId() => id;
    public string GetEmail() => email;
    public string GetName() => name;
}










enum TaskPriority
{
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}






enum TaskStatus
{
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
}







class ActivityLogger : ITaskObserver
{
    public void Update(Task task, string changeType)
    {
        Console.WriteLine($"LOGGER: Task '{task.GetTitle()}' was updated. Change: {changeType}");
    }
}


interface ITaskObserver
{
    void Update(Task task, string changeType);
}









class DoneState : TaskState
{
    public override void StartProgress(Task task)
    {
        Console.WriteLine("Cannot start a completed task. Reopen it first.");
    }

    public override void CompleteTask(Task task)
    {
        Console.WriteLine("Task is already done.");
    }

    public override void ReopenTask(Task task)
    {
        task.SetState(new TodoState());
    }

    public override TaskStatus GetStatus() => TaskStatus.DONE;
}








class InProgressState : TaskState
{
    public override void StartProgress(Task task)
    {
        Console.WriteLine("Task is already in progress.");
    }

    public override void CompleteTask(Task task)
    {
        task.SetState(new DoneState());
    }

    public override void ReopenTask(Task task)
    {
        task.SetState(new TodoState());
    }

    public override TaskStatus GetStatus() => TaskStatus.IN_PROGRESS;
}









abstract class TaskState
{
    public abstract void StartProgress(Task task);
    public abstract void CompleteTask(Task task);
    public abstract void ReopenTask(Task task);
    public abstract TaskStatus GetStatus();
}






class TodoState : TaskState
{
    public override void StartProgress(Task task)
    {
        task.SetState(new InProgressState());
    }

    public override void CompleteTask(Task task)
    {
        Console.WriteLine("Cannot complete a task that is not in progress.");
    }

    public override void ReopenTask(Task task)
    {
        Console.WriteLine("Task is already in TO-DO state.");
    }

    public override TaskStatus GetStatus() => TaskStatus.TODO;
}









class SortByDueDate : TaskSortStrategy
{
    public override void Sort(List<Task> tasks)
    {
        tasks.Sort((a, b) => string.Compare(a.GetDueDate(), b.GetDueDate(), StringComparison.Ordinal));
    }
}







class SortByPriority : TaskSortStrategy
{
    public override void Sort(List<Task> tasks)
    {
        // Higher priority comes first
        tasks.Sort((a, b) => b.GetPriority().CompareTo(a.GetPriority()));
    }
}




abstract class TaskSortStrategy
{
    public abstract void Sort(List<Task> tasks);
}









class TaskManagementSystem
{
    private static TaskManagementSystem instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, User> users = new Dictionary<string, User>();
    private readonly Dictionary<string, Task> tasks = new Dictionary<string, Task>();
    private readonly Dictionary<string, TaskList> taskLists = new Dictionary<string, TaskList>();

    private TaskManagementSystem() { }

    public static TaskManagementSystem GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new TaskManagementSystem();
                }
            }
        }
        return instance;
    }

    public User CreateUser(string name, string email)
    {
        User user = new User(name, email);
        users[user.GetId()] = user;
        return user;
    }

    public TaskList CreateTaskList(string listName)
    {
        TaskList taskList = new TaskList(listName);
        taskLists[taskList.GetId()] = taskList;
        return taskList;
    }

    public Task CreateTask(string title, string description, string dueDate,
                          TaskPriority priority, string createdByUserId)
    {
        if (!users.TryGetValue(createdByUserId, out User createdBy))
        {
            throw new ArgumentException("User not found.");
        }

        Task task = new TaskBuilder(title)
                .SetDescription(description)
                .SetDueDate(dueDate)
                .SetPriority(priority)
                .SetCreatedBy(createdBy)
                .Build();

        task.AddObserver(new ActivityLogger());

        tasks[task.GetId()] = task;
        return task;
    }

    public List<Task> ListTasksByUser(string userId)
    {
        if (!users.TryGetValue(userId, out User user))
        {
            return new List<Task>();
        }

        return tasks.Values.Where(task => task.GetAssignee() == user).ToList();
    }

    public List<Task> ListTasksByStatus(TaskStatus status)
    {
        return tasks.Values.Where(task => task.GetStatus() == status).ToList();
    }

    public void DeleteTask(string taskId)
    {
        tasks.Remove(taskId);
    }

    public List<Task> SearchTasks(string keyword, TaskSortStrategy sortingStrategy)
    {
        List<Task> matchingTasks = new List<Task>();
        foreach (var task in tasks.Values)
        {
            if (task.GetTitle().Contains(keyword) || task.GetDescription().Contains(keyword))
            {
                matchingTasks.Add(task);
            }
        }
        sortingStrategy.Sort(matchingTasks);
        return matchingTasks;
    }
}











using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class TaskManagementSystemDemo
{
    public static void Main(string[] args)
    {
        TaskManagementSystem taskManagementSystem = TaskManagementSystem.GetInstance();

        // Create users
        User user1 = taskManagementSystem.CreateUser("John Doe", "john@example.com");
        User user2 = taskManagementSystem.CreateUser("Jane Smith", "jane@example.com");

        // Create task lists
        TaskList taskList1 = taskManagementSystem.CreateTaskList("Enhancements");
        TaskList taskList2 = taskManagementSystem.CreateTaskList("Bug Fix");

        // Create tasks
        Task task1 = taskManagementSystem.CreateTask("Enhancement Task", "Launch New Feature",
                "2024-02-15", TaskPriority.LOW, user1.GetId());
        Task subtask1 = taskManagementSystem.CreateTask("Enhancement sub task", "Design UI/UX",
                "2024-02-14", TaskPriority.MEDIUM, user1.GetId());
        Task task2 = taskManagementSystem.CreateTask("Bug Fix Task", "Fix API Bug",
                "2024-02-16", TaskPriority.HIGH, user2.GetId());

        task1.AddSubtask(subtask1);

        taskList1.AddTask(task1);
        taskList2.AddTask(task2);

        taskList1.Display();

        // Update task status
        subtask1.StartProgress();

        // Assign task
        subtask1.SetAssignee(user2);

        taskList1.Display();

        // Search tasks
        List<Task> searchResults = taskManagementSystem.SearchTasks("Task", new SortByDueDate());
        Console.WriteLine("\nTasks with keyword Task:");
        foreach (Task task in searchResults)
        {
            Console.WriteLine(task.GetTitle());
        }

        // Filter tasks by status
        List<Task> filteredTasks = taskManagementSystem.ListTasksByStatus(TaskStatus.TODO);
        Console.WriteLine("\nTODO Tasks:");
        foreach (Task task in filteredTasks)
        {
            Console.WriteLine(task.GetTitle());
        }

        // Mark a task as done
        subtask1.CompleteTask();

        // Get tasks assigned to a user
        List<Task> userTaskList = taskManagementSystem.ListTasksByUser(user2.GetId());
        Console.WriteLine($"\nTask for {user2.GetName()}:");
        foreach (Task task in userTaskList)
        {
            Console.WriteLine(task.GetTitle());
        }

        taskList1.Display();

        // Delete a task
        taskManagementSystem.DeleteTask(task2.GetId());
    }
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































