import java.sql.SQLOutput;

public class Main {
    public static void main(String[] args) {

        Thread task1 = new Thread(new Task("Task A"));
        Thread task2 = new Thread(new Task("Task B"));
        Thread task3 = new Thread(new Task("Task C"));

        task1.start();
        task2.start();
        task3.start();

    }
}