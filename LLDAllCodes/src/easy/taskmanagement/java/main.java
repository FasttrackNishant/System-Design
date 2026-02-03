package easy.snakeandladder.java;

class ActivityLog {
    private final String description;
    private final LocalDateTime timestamp;

    public ActivityLog(String description) {
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + description;
    }
}







class Comment {
    private final String id;
    private final String content;
    private final User author;
    private final Date timestamp;

    public Comment(String content, User author) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.author = author;
        this.timestamp = new Date();
    }

    public User getAuthor() {
        return author;
    }
}




class Tag {
    private final String name;

    public Tag(String name) { this.name = name; }

    public String getName() { return name; }
}










class Task {
    private final String id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private TaskPriority priority;
    private final User createdBy;
    private User assignee;
    private TaskState currentState;
    private final Set<Tag> tags;
    private final List<Comment> comments;
    private final List<Task> subtasks;
    private final List<ActivityLog> activityLogs;
    private final List<TaskObserver> observers;

    private Task(TaskBuilder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority;
        this.createdBy = builder.createdBy;
        this.assignee = builder.assignee;
        this.tags = builder.tags;
        this.currentState = new TodoState(); // Initial state
        this.comments = new ArrayList<>();
        this.subtasks = new ArrayList<>();
        this.activityLogs = new ArrayList<>();
        this.observers = new ArrayList<>();
        addLog("Task created with title: " + title);
    }

    public synchronized void setAssignee(User user) {
        this.assignee = user;
        addLog("Assigned to " + user.getName());
        notifyObservers("assignee");
    }

    public synchronized void updatePriority(TaskPriority priority) {
        this.priority = priority;
        notifyObservers("priority");
    }

    public synchronized void addComment(Comment comment) {
        comments.add(comment);
        addLog("Comment added by " + comment.getAuthor().getName());
        notifyObservers("comment");
    }

    public synchronized void addSubtask(Task subtask) {
        subtasks.add(subtask);
        addLog("Subtask added: " + subtask.getTitle());
        notifyObservers("subtask_added");
    }

    // --- State Pattern Methods ---
    public void setState(TaskState state) {
        this.currentState = state;
        addLog("Status changed to: " + state.getStatus());
        notifyObservers("status");
    }
    public void startProgress() { currentState.startProgress(this); }
    public void completeTask() { currentState.completeTask(this); }
    public void reopenTask() { currentState.reopenTask(this); }

    // --- Observer Pattern Methods ---
    public void addObserver(TaskObserver observer) { observers.add(observer); }
    public void removeObserver(TaskObserver observer) { observers.remove(observer); }
    public void notifyObservers(String changeType) {
        for (TaskObserver observer : observers) {
            observer.update(this, changeType);
        }
    }
    public void addLog(String logDescription) {
        this.activityLogs.add(new ActivityLog(logDescription));
    }

    public boolean isComposite() { return !subtasks.isEmpty(); }

    public void display(String indent) {
        System.out.println(indent + "- " + title + " [" + getStatus() + ", " + priority + ", Due: " + dueDate + "]");
        if (isComposite()) {
            for (Task subtask : subtasks) {
                subtask.display(indent + "  ");
            }
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }
    public TaskPriority getPriority() {
        return priority;
    }
    public LocalDate getDueDate() {
        return dueDate;
    }
    public User getAssignee() {
        return assignee;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return currentState.getStatus();
    }

    // --- Builder Pattern ---
    public static class TaskBuilder {
        private final String id;
        private String title;
        private String description = "";
        private LocalDate dueDate;
        private TaskPriority priority;
        private User createdBy;
        private User assignee;
        private Set<Tag> tags;

        public TaskBuilder(String title) {
            this.id = UUID.randomUUID().toString();
            this.title = title;
        }

        public TaskBuilder description(String description) { this.description = description; return this; }
        public TaskBuilder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public TaskBuilder priority(TaskPriority priority) { this.priority = priority; return this; }
        public TaskBuilder assignee(User assignee) { this.assignee = assignee; return this; }
        public TaskBuilder createdBy(User createdBy) { this.createdBy = createdBy; return this; }
        public TaskBuilder tags(Set<Tag> tags) { this.tags = tags; return this; }

        public Task build() {
            return new Task(this);
        }
    }
}







class TaskList {
    private final String id;
    private final String name;
    private final List<Task> tasks;

    public TaskList(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.tasks = new CopyOnWriteArrayList<>();
    }

    public void addTask(Task task) {
        this.tasks.add(task);
    }

    public List<Task> getTasks() {
        return new ArrayList<>(tasks); // Return a copy to prevent external modification
    }

    // Getters...
    public String getId() { return id; }
    public String getName() { return name; }

    public void display() {
        System.out.println("--- Task List: " + name + " ---");
        for (Task task : tasks) {
            task.display("");
        }
        System.out.println("-----------------------------------");
    }
}





class User {
    private final String id;
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

    // Getters...
    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}








enum TaskPriority {
    LOW, 
    MEDIUM,
    HIGH,
    CRITICAL
}









enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
}













class ActivityLogger implements TaskObserver {
    @Override
    public void update(Task task, String changeType) {
        System.out.println("LOGGER: Task '" + task.getTitle() + "' was updated. Change: " + changeType);
    }
}






interface TaskObserver {
    void update(Task task, String changeType);
}






class DoneState implements TaskState {
    @Override
    public void startProgress(Task task) {
        System.out.println("Cannot start a completed task. Reopen it first.");
    }
    @Override
    public void completeTask(Task task) {
        System.out.println("Task is already done.");
    }
    @Override
    public void reopenTask(Task task) {
        task.setState(new TodoState());
    }
    @Override
    public TaskStatus getStatus() { return TaskStatus.DONE; }
}





class InProgressState implements TaskState {
    @Override
    public void startProgress(Task task) {
        System.out.println("Task is already in progress.");
    }
    @Override
    public void completeTask(Task task) {
        task.setState(new DoneState());
    }
    @Override
    public void reopenTask(Task task) {
        task.setState(new TodoState());
    }
    @Override
    public TaskStatus getStatus() { return TaskStatus.IN_PROGRESS; }
}



interface TaskState {
    void startProgress(Task task);
    void completeTask(Task task);
    void reopenTask(Task task);
    TaskStatus getStatus();
}








class TodoState implements TaskState {
    @Override
    public void startProgress(Task task) {
        task.setState(new InProgressState());
    }
    @Override
    public void completeTask(Task task) {
        System.out.println("Cannot complete a task that is not in progress.");
    }
    @Override
    public void reopenTask(Task task) {
        System.out.println("Task is already in TO-DO state.");
    }
    @Override
    public TaskStatus getStatus() { return TaskStatus.TODO; }
}






class SortByDueDate implements TaskSortStrategy {
    @Override
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getDueDate));
    }
}






class SortByPriority implements TaskSortStrategy {
    @Override
    public void sort(List<Task> tasks) {
        // Higher priority (lower enum ordinal) comes first
        tasks.sort(Comparator.comparing(Task::getPriority).reversed());
    }
}




interface TaskSortStrategy {
    void sort(List<Task> tasks);
}




class TaskManagementSystem {
    private static TaskManagementSystem instance;
    private final Map<String, User> users;
    private final Map<String, Task> tasks;
    private final Map<String, TaskList> taskLists;

    private TaskManagementSystem() {
        users = new ConcurrentHashMap<>();
        tasks = new ConcurrentHashMap<>();
        taskLists = new ConcurrentHashMap<>();
    }

    public static synchronized TaskManagementSystem getInstance() {
        if (instance == null) {
            instance = new TaskManagementSystem();
        }
        return instance;
    }

    public User createUser(String name, String email) {
        User user = new User(name, email);
        users.put(user.getId(), user);
        return user;
    }

    public TaskList createTaskList(String listName) {
        TaskList taskList = new TaskList(listName);
        taskLists.put(taskList.getId(), taskList);
        return taskList;
    }

    public Task createTask(String title, String description, LocalDate dueDate,
                           TaskPriority priority, String createdByUserId) {
        User createdBy = users.get(createdByUserId);
        if (createdBy == null)
            throw new IllegalArgumentException("User not found.");

        Task task = new Task.TaskBuilder(title)
                .description(description)
                .dueDate(dueDate)
                .priority(priority)
                .createdBy(createdBy)
                .build();

        task.addObserver(new ActivityLogger());

        tasks.put(task.getId(), task);
        return task;
    }

    public List<Task> listTasksByUser(String userId) {
        User user = users.get(userId);
        List<Task> result = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.getAssignee().equals(user)) {
                result.add(task);
            }
        }
        return result;                
    }

    public List<Task> listTasksByStatus(TaskStatus status) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.getStatus() == status) {
                result.add(task);
            }
        }
        return result;
    }

    public void deleteTask(String taskId) {
        tasks.remove(taskId);
    }

    public List<Task> searchTasks(String keyword, TaskSortStrategy sortingStrategy) {
        List<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.getTitle().contains(keyword) || task.getDescription().contains(keyword)) {
                matchingTasks.add(task);
            }
        }
        sortingStrategy.sort(matchingTasks);
        return matchingTasks;
    }
}












import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TaskManagementSystemDemo {
    public static void main(String[] args) {
        TaskManagementSystem taskManagementSystem = TaskManagementSystem.getInstance();

        // Create users
        User user1 = taskManagementSystem.createUser("John Doe", "john@example.com");
        User user2 = taskManagementSystem.createUser("Jane Smith", "jane@example.com");

        // Create task lists
        TaskList taskList1 = taskManagementSystem.createTaskList("Enhancements");
        TaskList taskList2 = taskManagementSystem.createTaskList("Bug Fix");

        // Create tasks
        Task task1 = taskManagementSystem.createTask("Enhancement Task", "Launch New Feature",
                LocalDate.now().plusDays(2), TaskPriority.LOW, user1.getId());
        Task subtask1 = taskManagementSystem.createTask( "Enhancement sub task", "Design UI/UX",
                LocalDate.now().plusDays(1), TaskPriority.MEDIUM, user1.getId());
        Task task2 = taskManagementSystem.createTask("Bug Fix Task", "Fix API Bug",
                LocalDate.now().plusDays(3), TaskPriority.HIGH, user2.getId());

        task1.addSubtask(subtask1);

        taskList1.addTask(task1);
        taskList2.addTask(task2);

        taskList1.display();

        // Update task status
        subtask1.startProgress();

        // Assign task
        subtask1.setAssignee(user2);

        taskList1.display();

        // Search tasks
        List<Task> searchResults = taskManagementSystem.searchTasks("Task", new SortByDueDate());
        System.out.println("\nTasks with keyword Task:");
        for (Task task : searchResults) {
            System.out.println(task.getTitle());
        }

        // Filter tasks by status
        List<Task> filteredTasks = taskManagementSystem.listTasksByStatus(TaskStatus.TODO);
        System.out.println("\nTODO Tasks:");
        for (Task task : filteredTasks) {
            System.out.println(task.getTitle());
        }

        // Mark a task as done
        subtask1.completeTask();

        // Get tasks assigned to a user
        List<Task> userTaskList = taskManagementSystem.listTasksByUser(user2.getId());
        System.out.println("\nTask for " + user2.getName() + ":");
        for (Task task : userTaskList) {
            System.out.println(task.getTitle());
        }

        taskList1.display();

        // Delete a task
        taskManagementSystem.deleteTask(task2.getId());
    }
}























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































