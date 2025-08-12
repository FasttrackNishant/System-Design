package aircraft;

public class SingletonRace {

    private static SingletonRace instance;

    private SingletonRace() {
        System.out.println("Singleton ctor ke andar hu");
    }

    public static SingletonRace getInstance() {
        if (instance == null) {
            instance = new SingletonRace();
        }
        return instance;
    }

    public static void main(String[] args) {
        Thread th1 = new Thread(() -> {
            SingletonRace.getInstance();
        });
        Thread th2 = new Thread(() -> {
            SingletonRace.getInstance();
        });

        th1.start();
        th2.start();
    }
}
