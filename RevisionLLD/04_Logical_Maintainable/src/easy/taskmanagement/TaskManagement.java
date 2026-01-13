package easy.taskmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

enum TaskStatus{
    TODO,
    IN_PROGRESS,
    DONE,
    BLOCKED
}

enum TaskPriority{
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

class User{
    private String id;
    private String name;
    private String email;

    public  User(String name , String email){
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

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

class Tag{
    private String name;

    public Tag(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

class Comment{
    private String id;
    private String content;
    private User user;
    private Date timeStamp;

    public Comment(String content,User author){

        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.user = author;
        this.timeStamp = new Date();

    }
}

class ActivityLog{
    private String description;
    private LocalDateTime timestamp;

    public ActivityLog(String description){
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + description;
    }
}

class Task {
    private String id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String Title;
    private TaskPriority priority;
    private TaskState currentState;
    private Set<Tag> tags;
    private List<Comment> comments;
    private List<Task> subtasks;
    private List<ActivityLog> activityLogs;
    private List<TaskObserver> observers;
    private User createdBy;
    private User assignee;


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

    public String getId(){
        return this.id;
    }

    public TaskPriority getPriority() {
        return this.priority;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public String getTitle() {
        return this.title;
    }

    public void setState(TaskState state) {
        this.currentState = state;
        addLog("Status changed to: " + state.getStatus());
        notifyObservers("status");
    }

    public void display(String indent) {
        System.out.println(indent + "- " + title + " [" + getStatus() + ", " + priority + ", Due: " + dueDate + "]");
    }
    public TaskStatus getStatus() {
        return currentState.getStatus();
    }


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

        public TaskBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TaskBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public TaskBuilder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }

        public TaskBuilder assignee(User assignee) {
            this.assignee = assignee;
            return this;
        }

        public TaskBuilder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public TaskBuilder tags(Set<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public Task build() {
            return new Task(this);
        }

    }


}

class TaskList{
    private String id;
    private String name;
    private List<Task> tasks;

    public TaskList(String name){
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.tasks = new ArrayList<>();
    }

    public String getId(){
        return this.id;
    }

}

interface TaskSortStrategy{
    void sort(List<Task> tasks);
}

class SortByPriority implements TaskSortStrategy{

    @Override
    public void sort(List<Task> tasks){
        tasks.sort(Comparator.comparing(Task::getPriority).reversed());
    }
}

class SortByDueDate implements TaskSortStrategy{

    @Override
    public void sort(List<Task> tasks){
        tasks.sort(Comparator.comparing(Task::getDueDate).reversed());
    }
}

interface TaskObserver{
    void update(Task task,String changeType);
}

class ActivityLogger implements TaskObserver{
    @Override
    public void update(Task task, String changeType) {
        System.out.println("LOGGER: Task '" + task.getTitle() + "' was updated. Change: " + changeType);
    }
}

interface TaskState{
    void startProgress(Task task);
    void completeTask(Task task);
    void reOpenTask(Task task);
    TaskStatus getStatus();
}

class TodoState implements TaskState
{

    @Override
    public void startProgress(Task task) {
        task.setState(new InProgressState());
    }

    @Override
    public void completeTask(Task task) {
        System.out.println("Cannot complete a task that is not in progress.");
    }

    @Override
    public void reOpenTask(Task task) {
        System.out.println("Task is already in TO-DO state.");
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.TODO;
    }
}

class InProgressState implements TaskState{

    @Override
    public void startProgress(Task task) {
        System.out.println("Task is already in progress.");
    }

    @Override
    public void completeTask(Task task) {
        task.setState(new CompletedState());
    }

    @Override
    public void reOpenTask(Task task) {
        task.setState(new TodoState());
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.IN_PROGRESS;
    }
}

class CompletedState implements TaskState{

    @Override
    public void startProgress(Task task) {
        System.out.println("Cannot start a completed task. Reopen it first.");
    }

    @Override
    public void completeTask(Task task) {
        System.out.println("Task is already done.");
    }

    @Override
    public void reOpenTask(Task task) {
        task.setState(new TodoState());
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.DONE;
    }
}

class TaskManagementSystem{
    private static TaskManagementSystem instance;

    private final Map<String,User> users;
    private final Map<String,Task> tasks;
    private final Map<String,TaskList> taskLists;

    private TaskManagementSystem(){
        users = new ConcurrentHashMap<>();
        tasks = new ConcurrentHashMap<>();
        taskLists = new ConcurrentHashMap<>();
    }

    public static synchronized TaskManagementSystem getInstance(){
        if(instance == null){
            instance = new TaskManagementSystem();
        }
        return instance;
    }

    public User createUser(String userId,String name , String email){

        User newUser = new User(name,email);
        users.put(userId,newUser);
        return newUser;
    }

    public TaskList createTaskList(String taskListName){
        TaskList newTaskList = new TaskList(taskListName);
        taskLists.put(newTaskList.getId(),newTaskList);
        return newTaskList;
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

    public void display() {
        System.out.println("--- Task List: " + tasks + " ---");

        System.out.println("-----------------------------------");
    }


}

class Main{
    public static void main(String[] args) {

        TaskManagementSystem system =  TaskManagementSystem.getInstance();

        system.createUser("3","dev","dfdf");
        system.createTask("test","This is descprition",LocalDate.now().plusDays(2),TaskPriority.HIGH,"3");

        system.display();


    }
}








