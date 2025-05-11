public class Main {
    public static void main(String[] args) {

        System.out.println("Hello, World!");

        ATCTower atcTower = new ATCTower();

        Indigo indigo = new Indigo(atcTower);

        indigo.requestPermission();

    }
}