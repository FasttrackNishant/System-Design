public class SingleTonRace {

    private static SingleTonRace instance;

    private SingleTonRace() {
        System.out.println("Constructor ke andar hu");
    }

    public static SingleTonRace getInstance() {
        if (instance == null) {
            instance = new SingleTonRace();
        }
        return instance;
    }

    public static void main(String[] args) {


        Thread th1 = new Thread(SingleTonRace::getInstance);
        Thread th2 = new Thread(SingleTonRace::getInstance);

        th1.start();
        th2.start();
    }
}
