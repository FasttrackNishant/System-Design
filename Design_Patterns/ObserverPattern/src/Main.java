public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        ATCTower tower = new ATCTower();

        Boeing boeing = new Boeing(tower);

        boeing.fly();
//        boeing.land();

        tower.notifyObserver();


    }
}